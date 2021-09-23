package com.livelike.engagementsdk.widget.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.StateListDrawable
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.databinding.WidgetAskMeAnythingBinding
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.WidgetsTheme
import com.livelike.engagementsdk.widget.viewModel.BaseViewModel
import com.livelike.engagementsdk.widget.viewModel.TextAskViewModel
import com.livelike.engagementsdk.widget.viewModel.TextAskWidget
import com.livelike.engagementsdk.widget.viewModel.WidgetStates


class TextAskView(context: Context, attr: AttributeSet? = null) : SpecifiedWidgetView(context, attr) {

    private var viewModel: TextAskViewModel? = null
    private var inflated = false
    private lateinit var binding: WidgetAskMeAnythingBinding

    override var dismissFunc: ((DismissAction) -> Unit)? = { viewModel?.dismissWidget(it) }

    override var widgetViewModel: BaseViewModel? = null
        set(value) {
            field = value
            viewModel = value as TextAskViewModel
        }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewModel?.data?.subscribe(javaClass.simpleName) { resourceObserver(it) }
        viewModel?.widgetState?.subscribe(javaClass) { stateWidgetObserver(it) }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewModel?.data?.unsubscribe(javaClass.simpleName)
        viewModel?.widgetState?.unsubscribe(javaClass.simpleName)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun resourceObserver(widget: TextAskWidget?) {
        widget?.apply {
            if (!inflated) {
                inflated = true
                binding = WidgetAskMeAnythingBinding.inflate(LayoutInflater.from(context), this@TextAskView, true)
            }

            binding.titleView.text = resource.title
            binding.bodyText.text = resource.prompt
            binding.confirmationMessageTv.text = resource.confirmation_message
            binding.confirmationMessageTv.visibility = INVISIBLE

            binding.userInputEdt.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(arg0: Editable) {
                    if (binding.userInputEdt.isEnabled) enableSendBtn()
                }

                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    val text: String = binding.userInputEdt.text.toString()
                    if (text.startsWith(" ")) {
                        binding.userInputEdt.setText(text.trim { it <= ' ' })
                    }else {
                        binding.textCount.text = (240 - s.length).toString()
                    }
                    binding.textCount.visibility = View.VISIBLE
                }
            })

            binding.userInputEdt.setOnTouchListener { v, _ -> // Disallow the touch request for parent scroll on touch of child view
                if (binding.userInputEdt.text.toString().isNotEmpty()) {
                    v.parent.requestDisallowInterceptTouchEvent(true)
                }
                false
            }

            binding.sendBtn.setOnClickListener {
                if (binding.userInputEdt.text.toString().trim().isNotEmpty()) {
                    lockInteractionAndSubmitResponse()
                    hideKeyboard()
                }
            }

            if (viewModel?.getUserInteraction() != null) {
                disableSendBtn()
                disableUserInput()
                binding.confirmationMessageTv.visibility = VISIBLE
                binding.textCount.visibility = GONE
                viewModel?.getUserInteraction()?.text?.let {
                    binding.userInputEdt.setText(viewModel?.getUserInteraction()?.text)
                }
            }

            // by default send button will be disabled
            disableSendBtn()
            setImeOptionDoneInKeyboard()
            widgetsTheme?.let {
                applyTheme(it)
            }

            if (widgetViewModel?.widgetState?.latest() == null || widgetViewModel?.widgetState?.latest() == WidgetStates.READY)
                widgetViewModel?.widgetState?.onNext(WidgetStates.READY)
            logDebug { "showing text ask Widget" }
        }
        if (widget == null) {
            inflated = false
            removeAllViews()
            parent?.let { (it as ViewGroup).removeAllViews() }
        }
    }

    private fun stateWidgetObserver(widgetStates: WidgetStates?) {

        when (widgetStates) {
            WidgetStates.READY -> {
            }

            WidgetStates.INTERACTING -> {
                unLockInteraction()
                showResultAnimation = true
                viewModel?.data?.latest()?.resource?.timeout?.let { timeout ->
                    showTimer(
                        timeout, binding.textEggTimer,
                        {
                            viewModel?.animationEggTimerProgress = it
                        },
                        {
                            hideKeyboard()
                            viewModel?.dismissWidget(it)
                        }
                    )
                }
            }
            WidgetStates.RESULTS, WidgetStates.FINISHED -> {
            }
        }
        if (viewModel?.enableDefaultWidgetTransition == true) {
            defaultStateTransitionManager(widgetStates)
        }
    }

    private fun defaultStateTransitionManager(widgetStates: WidgetStates?) {
        when (widgetStates) {
            WidgetStates.READY -> {
                moveToNextState()
            }
            WidgetStates.INTERACTING -> {
                viewModel?.data?.latest()?.let {
                    viewModel?.startDismissTimout(it.resource.timeout)
                }
            }
            WidgetStates.RESULTS -> {
                viewModel?.confirmationState()
            }

            WidgetStates.FINISHED -> {
                resourceObserver(null)
            }
        }
    }

    override fun applyTheme(theme: WidgetsTheme) {
        super.applyTheme(theme)
        widgetsTheme?.textAsk?.let { themeComponent ->
            // title
            AndroidResource.updateThemeForView(
                binding.titleView,
                themeComponent.title,
                fontFamilyProvider
            )
            if (themeComponent.header?.background != null) {
                binding.txtTitleBackground.background = AndroidResource.createDrawable(themeComponent.header)
            }
            themeComponent.header?.padding?.let {
                AndroidResource.setPaddingForView(binding.titleView, themeComponent.header.padding)
            }

            // prompt
            AndroidResource.updateThemeForView(
                binding.bodyText,
                themeComponent.prompt,
                fontFamilyProvider
            )

            // confirmation message
            AndroidResource.updateThemeForView(
                binding.confirmationMessageTv,
                themeComponent.confirmation,
                fontFamilyProvider
            )

            // widget container background
            AndroidResource.createDrawable(themeComponent.body)?.let {
                binding.widgetContainer.background = it
            }
            themeComponent.body?.padding?.let {
                AndroidResource.setPaddingForView(binding.widgetContainer, themeComponent.body.padding)
            }

            // user input text
            AndroidResource.updateThemeForView(
                binding.userInputEdt,
                themeComponent.replyEnabled,
                fontFamilyProvider
            )
            // user input text padding
            themeComponent.replyEnabled?.padding?.let {
                AndroidResource.setPaddingForView(binding.userInputEdt, themeComponent.replyEnabled.padding)
            }

            // submit button drawables with state
            val submitButtonEnabledDrawable = AndroidResource.createDrawable(
                themeComponent.submitButtonEnabled
            )
            val submitButtonDisabledDrawable = AndroidResource.createDrawable(
                themeComponent.submitButtonDisabled
            )
            val state = StateListDrawable()
            state.addState(intArrayOf(android.R.attr.state_enabled), submitButtonEnabledDrawable)
            state.addState(intArrayOf(), submitButtonDisabledDrawable)
            binding.sendBtn.background = state

            // user input with state
            if (themeComponent.replyEnabled?.background != null &&
                themeComponent.replyDisabled?.background != null
            ) {
                val userInputEnabledDrawable = AndroidResource.createDrawable(
                    themeComponent.replyEnabled
                )
                val userInputDisabledDrawable = AndroidResource.createDrawable(
                    themeComponent.replyDisabled
                )
                val inputState = StateListDrawable()
                inputState.addState(intArrayOf(android.R.attr.state_enabled), userInputEnabledDrawable)
                inputState.addState(intArrayOf(), userInputDisabledDrawable)
                binding.userInputEdt.background = inputState
            }
        }
    }

    /** disables the send button */
    private fun disableSendBtn() {
        binding.sendBtn.isEnabled = false
    }

    /** enables the send button */
    private fun enableSendBtn() {
        val isReady: Boolean = binding.userInputEdt.text.toString().isNotEmpty()
        binding.sendBtn.isEnabled = isReady
    }

    /** disables the user input box */
    private fun disableUserInput() {
        // binding.userInputEdt.isEnabled = false
        binding.userInputEdt.isFocusableInTouchMode = false
        binding.userInputEdt.isCursorVisible = false
        binding.userInputEdt.clearFocus()
    }

    /** changes the return key as done in keyboard */
    private fun setImeOptionDoneInKeyboard() {
        binding.userInputEdt.imeOptions = EditorInfo.IME_ACTION_DONE
        binding.userInputEdt.setRawInputType(InputType.TYPE_CLASS_TEXT)
    }

    private fun unLockInteraction() {
        // marked widget as interactive
        viewModel?.markAsInteractive()
    }

    /** gets called when send button is clicked to lock
     * the response submitted*/
    private fun lockInteractionAndSubmitResponse() {
        disableUserInput()
        disableSendBtn()
        binding.textCount.visibility = View.GONE
        viewModel?.lockAndSubmitReply(binding.userInputEdt.text.toString().trim())?.let {
            binding.confirmationMessageTv.visibility = VISIBLE
        }
    }

    /**
     * this is used to hide default keyboard
     **/
    private fun hideKeyboard() {
        val inputManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(
            binding.userInputEdt.windowToken,
            0
        )
    }
}
