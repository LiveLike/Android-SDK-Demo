@file:Suppress("UNNECESSARY_SAFE_CALL", "UNCHECKED_CAST")

package com.livelike.engagementsdk.widget.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.model.Resource
import com.livelike.engagementsdk.widget.utils.livelikeSharedPrefs.shouldShowPointTutorial
import com.livelike.engagementsdk.widget.viewModel.BaseViewModel
import com.livelike.engagementsdk.widget.viewModel.WidgetState
import com.livelike.engagementsdk.widget.viewModel.WidgetStates
import com.livelike.engagementsdk.widget.viewModel.WidgetViewModel
import kotlinx.android.synthetic.main.common_lock_btn_lay.view.lay_lock
import kotlinx.android.synthetic.main.widget_text_option_selection.view.pointView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.progressionMeterView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.textEggTimer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * For now creating separate class, will mere it with specified widget view after full assessment of other widget views and then move all widget views to inherit this
 * Also For now Doing minimal refactor to expedite image slider delivery.
 */

internal abstract class GenericSpecifiedWidgetView<Entity : Resource, T : WidgetViewModel<Entity>>(
    context: Context,
    attr: AttributeSet? = null
) : SpecifiedWidgetView(context, attr) {

    // Move viewmodel to constructor
    lateinit var viewModel: T

    override var dismissFunc: ((DismissAction) -> Unit)? = { viewModel?.dismissWidget(it) }

    var isViewInflated = false
    var isFirstInteraction = false

    override var widgetViewModel: BaseViewModel? = null
        get() = viewModel
        set(value) {
            field = value
            viewModel = value as T
//            subscribeCalls()
        }

    protected open fun stateObserver(widgetState: WidgetState) {
        when (widgetState) {
            WidgetState.LOCK_INTERACTION -> confirmInteraction()
            WidgetState.SHOW_RESULTS -> showResults()
            WidgetState.SHOW_GAMIFICATION -> {
                rewardsObserver()
                if (viewModel?.enableDefaultWidgetTransition) {
                    viewModel?.uiScope.launch {
                        delay(2000)
                        viewModel?.dismissWidget(DismissAction.TIMEOUT)
                    }
                }
            }
            WidgetState.DISMISS -> {
                dataModelObserver(null)
            }
        }
    }

    protected abstract fun showResults()

    protected abstract fun confirmInteraction()

    protected open fun dataModelObserver(entity: Entity?) {
        entity?.let { _ ->
            if (!isViewInflated) {
                isViewInflated = true
                if (widgetViewModel?.widgetState?.latest() == null)
                    widgetViewModel?.widgetState?.onNext(WidgetStates.READY)
            }
        }
        if (entity == null) {
            isViewInflated = false
            removeAllViews()
            parent?.let { (it as ViewGroup).removeAllViews() }
        }
    }

    private fun rewardsObserver() {
        viewModel.gamificationProfile?.latest()?.let {
            if (!shouldShowPointTutorial() && it.newPoints > 0) {
                pointView.startAnimation(it.newPoints, true)
                wouldShowProgressionMeter(viewModel?.rewardsType, it, progressionMeterView)
            }
        }
    }

    protected open fun subscribeCalls() {
        viewModel.data.subscribe(javaClass.simpleName) {
            dataModelObserver(it)
        }
        viewModel.state.subscribe(javaClass.simpleName) {
            it?.let { stateObserver(it) }
        }
        widgetViewModel?.widgetState?.subscribe(javaClass.simpleName) {
            when (it) {
                WidgetStates.READY -> {
                    isFirstInteraction = false
                    lockInteraction()
                }
                WidgetStates.INTERACTING -> {
                    unLockInteraction()
                    showResultAnimation = true
                    viewModel?.data?.latest()?.timeout?.let { timeout ->
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
                    viewModel?.results?.subscribe(javaClass.simpleName) {
                        if (isFirstInteraction)
                            showResults()
                    }
                }
                WidgetStates.RESULTS, WidgetStates.FINISHED -> {
                    lockInteraction()
                    onWidgetInteractionCompleted()
                    viewModel?.results?.subscribe(javaClass.simpleName) { showResults() }
                    // showResults()
                    viewModel.confirmInteraction()
                }
            }

            if (viewModel?.enableDefaultWidgetTransition) {
                defaultStateTransitionManager(it)
            }
        }
    }

    internal abstract fun lockInteraction()

    internal abstract fun unLockInteraction()

    private fun defaultStateTransitionManager(widgetStates: WidgetStates?) {
        when (widgetStates) {
            WidgetStates.READY -> {
                moveToNextState()
            }
            WidgetStates.INTERACTING -> {
                viewModel.data.latest()?.let { entity ->
                    val timeout = AndroidResource.parseDuration(entity.timeout)
                    viewModel.startInteractionTimeout(timeout)
                }
//            viewModel?.data?.latest()?.let {
//                viewModel?.startDismissTimout(it.resource.timeout)
//            }
            }
            WidgetStates.RESULTS -> {
//            viewModel?.confirmationState()
            }
            WidgetStates.FINISHED -> {
                dataModelObserver(null)
            }
        }
    }

    protected open fun unsubscribeCalls() {
        viewModel.state.unsubscribe(javaClass.name)
        viewModel.data.unsubscribe(javaClass.name)
        widgetViewModel?.widgetState?.unsubscribe(javaClass.name)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        subscribeCalls()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        unsubscribeCalls()
    }
}
