package com.livelike.engagementsdk.widget.view

import android.animation.Animator
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.widget.OptionsWidgetThemeComponent
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.WidgetsTheme
import com.livelike.engagementsdk.widget.adapters.WidgetOptionsViewAdapter
import com.livelike.engagementsdk.widget.model.Resource
import com.livelike.engagementsdk.widget.utils.livelikeSharedPrefs.shouldShowPointTutorial
import com.livelike.engagementsdk.widget.viewModel.BaseViewModel
import com.livelike.engagementsdk.widget.viewModel.QuizViewModel
import com.livelike.engagementsdk.widget.viewModel.QuizWidget
import com.livelike.engagementsdk.widget.viewModel.WidgetStates
import kotlinx.android.synthetic.main.atom_widget_title.view.titleTextView
import kotlinx.android.synthetic.main.common_lock_btn_lay.view.btn_lock
import kotlinx.android.synthetic.main.common_lock_btn_lay.view.label_lock
import kotlinx.android.synthetic.main.common_lock_btn_lay.view.lay_lock
import kotlinx.android.synthetic.main.widget_text_option_selection.view.followupAnimation
import kotlinx.android.synthetic.main.widget_text_option_selection.view.lay_textRecyclerView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.pointView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.progressionMeterView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.textEggTimer
import kotlinx.android.synthetic.main.widget_text_option_selection.view.textRecyclerView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.titleView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.txtTitleBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class QuizView(context: Context, attr: AttributeSet? = null) : SpecifiedWidgetView(context, attr) {

    private var viewModel: QuizViewModel? = null

    override var dismissFunc: ((action: DismissAction) -> Unit)? = { viewModel?.dismissWidget(it) }

    override var widgetViewModel: BaseViewModel? = null
        set(value) {
            field = value
            viewModel = value as QuizViewModel
        }

    private var isFirstInteraction = false

    init {
        isFirstInteraction = viewModel?.getUserInteraction() != null
    }

    // Refresh the view when re-attached to the activity
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewModel?.data?.subscribe(javaClass.simpleName) { resourceObserver(it) }
        viewModel?.widgetState?.subscribe(javaClass) { stateWidgetObserver(it) }
        viewModel?.currentVoteId?.subscribe(javaClass) { onClickObserver() }
        // viewModel?.results?.subscribe(javaClass) { resultsObserver(it) }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewModel?.data?.unsubscribe(javaClass.simpleName)
        viewModel?.widgetState?.unsubscribe(javaClass.simpleName)
        viewModel?.currentVoteId?.unsubscribe(javaClass.simpleName)
        viewModel?.results?.unsubscribe(javaClass.simpleName)
    }

    private fun stateWidgetObserver(widgetStates: WidgetStates?) {
        when (widgetStates) {
            WidgetStates.READY -> {
                lockInteraction()
            }
            WidgetStates.INTERACTING -> {
                unLockInteraction()
                showResultAnimation = true
                // show timer while widget interaction mode
                viewModel?.data?.latest()?.resource?.timeout?.let { timeout ->
                    showTimer(
                        timeout, textEggTimer,
                        {
                            viewModel?.animationEggTimerProgress = it
                        },
                        {
                            viewModel?.dismissWidget(it)
                        }
                    )
                }
                lay_lock.visibility = View.VISIBLE
            }
            WidgetStates.RESULTS, WidgetStates.FINISHED -> {
                lockInteraction()
                onWidgetInteractionCompleted()
                disableLockButton()
                label_lock.visibility = View.VISIBLE
                viewModel?.results?.subscribe(javaClass.simpleName) {
                    if (isFirstInteraction) {
                        resultsObserver(viewModel?.results?.latest())
                    }
                }

                if (isFirstInteraction) {
                    viewModel?.apply {
                        val isUserCorrect =
                            adapter?.selectedPosition?.let {
                                if (it > -1) {
                                    return@let adapter?.myDataset?.get(it)?.is_correct
                                }
                                return@let false
                            }
                                ?: false
                        val rootPath =
                            if (isUserCorrect) widgetViewThemeAttributes.widgetWinAnimation else widgetViewThemeAttributes.widgetLoseAnimation
                        animationPath =
                            AndroidResource.selectRandomLottieAnimation(rootPath, context) ?: ""
                    }

                    viewModel?.adapter?.correctOptionId =
                        viewModel?.adapter?.myDataset?.find { it.is_correct }?.id ?: ""
                    viewModel?.adapter?.userSelectedOptionId =
                        viewModel?.adapter?.selectedPosition?.let { it1 ->
                        if (it1 > -1)
                            return@let viewModel?.adapter?.myDataset?.get(it1)?.id
                        return@let null
                    } ?: ""

                    textRecyclerView.swapAdapter(viewModel?.adapter, false)
                    textRecyclerView.adapter?.notifyItemChanged(0)
                }

                followupAnimation.apply {
                    if (isFirstInteraction) {
                        setAnimation(viewModel?.animationPath)
                        progress = viewModel?.animationProgress ?: 0f
                        logDebug { "Animation: ${viewModel?.animationPath}" }
                        addAnimatorUpdateListener { valueAnimator ->
                            viewModel?.animationProgress = valueAnimator.animatedFraction
                        }
                        if (progress != 1f) {
                            resumeAnimation()
                        }
                        visibility = if (showResultAnimation) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                    } else {
                        visibility = View.GONE
                    }
                }

                viewModel?.points?.let {
                    if (!shouldShowPointTutorial() && it > 0) {
                        pointView.startAnimation(it, true)
                        wouldShowProgressionMeter(
                            viewModel?.rewardsType,
                            viewModel?.gamificationProfile?.latest(),
                            progressionMeterView
                        )
                    }
                }
            }
        }
        if (viewModel?.enableDefaultWidgetTransition == true) {
            defaultStateTransitionManager(widgetStates)
        }
    }

    private var inflated = false

    private fun onClickObserver() {
        viewModel?.onOptionClicked()
    }

    override fun applyTheme(theme: WidgetsTheme) {
        super.applyTheme(theme)
        viewModel?.data?.latest()?.let { widget ->
            theme.getThemeLayoutComponent(widget.type)?.let { themeComponent ->
                if (themeComponent is OptionsWidgetThemeComponent) {
                    applyThemeOnTitleView(themeComponent)
                    applyThemeOnTagView(themeComponent)
                    viewModel?.adapter?.component = themeComponent
                    viewModel?.adapter?.notifyDataSetChanged()
                    AndroidResource.createDrawable(themeComponent.body)?.let {
                        lay_textRecyclerView.background = it
                    }
                }
            }
        }
    }

    private fun resourceObserver(widget: QuizWidget?) {
        widget?.apply {
            val optionList = resource.getMergedOptions() ?: return
            if (!inflated) {
                inflated = true
                inflate(context, R.layout.widget_text_option_selection, this@QuizView)
            }

            // added tag for identification of widget (by default will be empty)
            setTagViewWithStyleChanges(context.resources.getString(R.string.livelike_quiz_tag))
            titleView.title = resource.question
            txtTitleBackground.setBackgroundResource(R.drawable.header_rounded_corner_quiz)
            lay_textRecyclerView.setBackgroundResource(R.drawable.body_rounded_corner_quiz)
            titleTextView.gravity = Gravity.START

            viewModel?.adapter = viewModel?.adapter ?: WidgetOptionsViewAdapter(optionList, type)

            // set on click
            viewModel?.adapter?.onClick = {
                viewModel?.adapter?.apply {
                    val currentSelectionId = myDataset[selectedPosition]
                    viewModel?.currentVoteId?.onNext(currentSelectionId.id)
                    widgetLifeCycleEventsListener?.onUserInteract(widgetData)
                    isFirstInteraction = true
                }
                enableLockButton()
            }

            widgetsTheme?.let {
                applyTheme(it)
            }
            disableLockButton()
            textRecyclerView.apply {
                isFirstInteraction = viewModel?.getUserInteraction() != null
                this.adapter = viewModel?.adapter
                viewModel?.adapter?.restoreSelectedPosition(viewModel?.getUserInteraction()?.choiceId)
                setHasFixedSize(true)
            }
            btn_lock.setOnClickListener {
                if (viewModel?.adapter?.selectedPosition != RecyclerView.NO_POSITION) {
                    lockVote()
                    textEggTimer.visibility = GONE
                }
            }
            if (viewModel?.getUserInteraction() != null) {
                findViewById<TextView>(R.id.label_lock)?.visibility = VISIBLE
            } else if (viewModel?.adapter?.selectedPosition != RecyclerView.NO_POSITION) {
                enableLockButton()
            }

            if (widgetViewModel?.widgetState?.latest() == null || widgetViewModel?.widgetState?.latest() == WidgetStates.READY)
                widgetViewModel?.widgetState?.onNext(WidgetStates.READY)
            logDebug { "showing QuizWidget" }
        }
        if (widget == null) {
            inflated = false
            removeAllViews()
            parent?.let { (it as ViewGroup).removeAllViews() }
        }
    }

    private fun lockVote() {
        disableLockButton()
        viewModel?.currentVoteId?.currentData?.let { id ->
            viewModel?.adapter?.myDataset?.find { it.id == id }?.let { option ->
                viewModel?.saveInteraction(option)
            }
        }
        label_lock.visibility = View.VISIBLE
        viewModel?.run {
            timeOutJob?.cancel()
            uiScope.launch {
                lockInteractionAndSubmitVote()
            }
        }
    }

    fun enableLockButton() {
        btn_lock.isEnabled = true
        btn_lock.alpha = 1f
    }

    fun disableLockButton() {
        lay_lock.visibility = VISIBLE
        btn_lock.isEnabled = false
        btn_lock.alpha = 0.5f
    }

    private fun lockInteraction() {
        viewModel?.adapter?.selectionLocked = true
    }

    private fun unLockInteraction() {
        viewModel?.adapter?.selectionLocked = false
        // marked widget as interactive
        viewModel?.markAsInteractive()
    }

    private fun defaultStateTransitionManager(widgetStates: WidgetStates?) {
        when (widgetStates) {
            WidgetStates.READY -> {
                moveToNextState()
            }
            WidgetStates.INTERACTING -> {
                viewModel?.data?.latest()?.let {
                    viewModel?.startDismissTimout(
                        it.resource.timeout,
                        widgetViewThemeAttributes
                    )
                }
            }
            WidgetStates.RESULTS -> {
                if (!isFirstInteraction) {
                    viewModel?.dismissWidget(DismissAction.TIMEOUT)
                }
                followupAnimation.apply {
                    addAnimatorListener(object : Animator.AnimatorListener {
                        override fun onAnimationRepeat(animation: Animator?) {
                        }

                        override fun onAnimationEnd(animation: Animator?) {
                            viewModel?.uiScope?.launch {
                                delay(11000)
                                viewModel?.dismissWidget(DismissAction.TIMEOUT)
                            }
                        }

                        override fun onAnimationCancel(animation: Animator?) {
                        }

                        override fun onAnimationStart(animation: Animator?) {
                        }
                    })
                }
            }
            WidgetStates.FINISHED -> {
                resourceObserver(null)
            }
        }
    }

    private fun resultsObserver(resource: Resource?) {
        (resource ?: viewModel?.data?.currentData?.resource)?.apply {
            val optionResults = this.getMergedOptions() ?: return
            val totalVotes = optionResults.sumBy { it.getMergedVoteCount().toInt() }
            val options = viewModel?.data?.currentData?.resource?.getMergedOptions() ?: return
            options.forEach { opt ->
                optionResults.find {
                    it.id == opt.id
                }?.apply {
                    opt.updateCount(this)
                    opt.percentage = opt.getPercent(totalVotes.toFloat())
                }
            }
            viewModel?.adapter?.myDataset = options
            textRecyclerView.swapAdapter(viewModel?.adapter, false)
            viewModel?.adapter?.showPercentage = false
            logDebug { "QuizWidget Showing result total:$totalVotes" }
        }
    }
}
