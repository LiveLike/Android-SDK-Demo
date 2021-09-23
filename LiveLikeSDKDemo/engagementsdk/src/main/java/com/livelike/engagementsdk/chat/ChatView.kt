package com.livelike.engagementsdk.chat

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnLayoutChangeListener
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.livelike.engagementsdk.CHAT_PROVIDER
import com.livelike.engagementsdk.DEFAULT_CHAT_MESSAGE_DATE_TIIME_FROMATTER
import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.KeyboardHideReason
import com.livelike.engagementsdk.KeyboardType
import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.ViewAnimationEvents
import com.livelike.engagementsdk.chat.data.remote.PubnubChatEventType
import com.livelike.engagementsdk.chat.stickerKeyboard.FragmentClickListener
import com.livelike.engagementsdk.chat.stickerKeyboard.Sticker
import com.livelike.engagementsdk.chat.stickerKeyboard.StickerKeyboardView
import com.livelike.engagementsdk.chat.stickerKeyboard.countMatches
import com.livelike.engagementsdk.chat.stickerKeyboard.findImages
import com.livelike.engagementsdk.chat.stickerKeyboard.replaceWithImages
import com.livelike.engagementsdk.chat.stickerKeyboard.replaceWithStickers
import com.livelike.engagementsdk.chat.stickerKeyboard.targetByteArrays
import com.livelike.engagementsdk.chat.stickerKeyboard.targetDrawables
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.AndroidResource.Companion.dpToPx
import com.livelike.engagementsdk.core.utils.animators.buildScaleAnimator
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.core.utils.logError
import com.livelike.engagementsdk.core.utils.scanForActivity
import com.livelike.engagementsdk.publicapis.LiveLikeChatMessage
import com.livelike.engagementsdk.publicapis.toLiveLikeChatMessage
import com.livelike.engagementsdk.widget.data.models.ProgramGamificationProfile
import com.livelike.engagementsdk.widget.view.loadImage
import kotlinx.android.synthetic.main.chat_input.view.button_chat_send
import kotlinx.android.synthetic.main.chat_input.view.button_emoji
import kotlinx.android.synthetic.main.chat_input.view.chat_input_background
import kotlinx.android.synthetic.main.chat_input.view.chat_input_border
import kotlinx.android.synthetic.main.chat_input.view.edittext_chat_message
import kotlinx.android.synthetic.main.chat_input.view.user_profile_display_LL
import kotlinx.android.synthetic.main.chat_user_profile_bar.view.gamification_badge_iv
import kotlinx.android.synthetic.main.chat_user_profile_bar.view.pointView
import kotlinx.android.synthetic.main.chat_user_profile_bar.view.rank_label
import kotlinx.android.synthetic.main.chat_user_profile_bar.view.rank_value
import kotlinx.android.synthetic.main.chat_user_profile_bar.view.user_profile_tv
import kotlinx.android.synthetic.main.chat_view.view.chatInput
import kotlinx.android.synthetic.main.chat_view.view.chat_view
import kotlinx.android.synthetic.main.chat_view.view.chatdisplay
import kotlinx.android.synthetic.main.chat_view.view.chatdisplayBack
import kotlinx.android.synthetic.main.chat_view.view.loadingSpinner
import kotlinx.android.synthetic.main.chat_view.view.snap_live
import kotlinx.android.synthetic.main.chat_view.view.sticker_keyboard
import kotlinx.android.synthetic.main.chat_view.view.swipeToRefresh
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pl.droidsonroids.gif.MultiCallback
import java.util.Date
import kotlin.math.max
import kotlin.math.min

/**
 *  This view will load and display a chat component. To use chat view
 *  ```
 *  <com.livelike.sdk.chat.ChatView
 *      android:id="@+id/chatView"
 *      android:layout_width="wrap_content"
 *      android:layout_height="wrap_content">
 *   </com.livelike.sdk.chat.ChatView>
 *  ```
 *
 */
open class ChatView(context: Context, private val attrs: AttributeSet?) :
    ConstraintLayout(context, attrs) {

    /**
     * use this variable to hide message input to build use case like influencer chat
     **/
    var isChatInputVisible: Boolean = true
        set(value) {
            field = value
            if (value) {
                chatInput.visibility = View.VISIBLE
            } else {
                chatInput.visibility = View.GONE
            }
        }

    private val chatAttribute = ChatViewThemeAttributes()
    private val uiScope = CoroutineScope(Dispatchers.Main)

    private var session: LiveLikeChatSession? = null
    private var snapToLiveAnimation: AnimatorSet? = null
    private var showingSnapToLive: Boolean = false
    private var currentUser: LiveLikeUser? = null

    var allowMediaFromKeyboard: Boolean = true
        set(value) {
            field = value
            edittext_chat_message.allowMediaFromKeyboard = value
        }

    var emptyChatBackgroundView: View? = null
        set(view) {
            field = view
            if (chatdisplayBack.childCount > 1)
                chatdisplayBack.removeViewAt(1)
            initEmptyView()
        }

    /** Boolean option to enable / disable the profile display inside chat view */
    var displayUserProfile: Boolean = false
        set(value) {
            field = value
            user_profile_display_LL?.apply {
                visibility = if (value) View.VISIBLE else View.GONE
            }
        }

    private val viewModel: ChatViewModel?
        get() = (session as ChatSession?)?.chatViewModel

    val callback = MultiCallback(true)
    private val layoutChangeListener =
        OnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            if (bottom < oldBottom) {
                viewModel?.chatAdapter?.itemCount?.let {
                    if (it > 0) {
                        if (viewModel?.isLastItemVisible == true) {
                            chatdisplay.post {
                                chatdisplay.smoothScrollToPosition(it - 1)
                            }
                        }
                    }
                }
            }
        }

    init {
        context.scanForActivity()?.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        ) // INFO: Adjustresize doesn't work with Fullscreen app.. See issue https://stackoverflow.com/questions/7417123/android-how-to-adjust-layout-in-full-screen-mode-when-softkeyboard-is-visible
        context.obtainStyledAttributes(
            attrs,
            R.styleable.ChatView,
            0, 0
        ).apply {
            try {
                displayUserProfile =
                    getBoolean(R.styleable.ChatView_displayUserProfile, false)
                chatAttribute.initAttributes(context, this)
            } finally {
                recycle()
            }
        }
        initView(context)
    }

    var enableChatMessageURLs: Boolean = false
        set(value) {
            field = value
            viewModel?.chatAdapter?.showLinks = value
        }

    var chatMessageUrlPatterns: String? = null
        set(value) {
            field = value
            value?.let {
                if (value.isNotEmpty())
                    viewModel?.chatAdapter?.linksRegex = it.toRegex()
            }
        }

    private fun setBackButtonInterceptor(v: View) {
        v.isFocusableInTouchMode = true
        v.requestFocus()
        v.setOnKeyListener(object : OnKeyListener {
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
                if (event?.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
                    if (sticker_keyboard.visibility == View.VISIBLE) {
                        hideStickerKeyboard(KeyboardHideReason.BACK_BUTTON)
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun initView(context: Context) {
        LayoutInflater.from(context).inflate(R.layout.chat_view, this, true)
        user_profile_display_LL.visibility = if (displayUserProfile) View.VISIBLE else View.GONE
        chatAttribute.apply {
            rank_value.setTextColor(rankValueTextColor)
            chat_view.background = chatViewBackgroundRes
            chatDisplayBackgroundRes?.let {
                chatdisplay.background = it
            }
            chat_input_background.background = chatInputViewBackgroundRes
            chat_input_border.background = chatInputBackgroundRes
            edittext_chat_message.setTextColor(chatInputTextColor)
            edittext_chat_message.setHintTextColor(chatInputHintTextColor)
            edittext_chat_message.setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                chatInputTextSize.toFloat()
            )
            button_emoji.setImageDrawable(chatStickerSendDrawable)
            button_emoji.setColorFilter(
                sendStickerTintColor,
                android.graphics.PorterDuff.Mode.MULTIPLY
            )
            button_emoji.visibility = when {
                showStickerSend -> View.VISIBLE
                else -> View.GONE
            }

            val layoutParams = button_chat_send.layoutParams
            layoutParams.width = sendIconWidth
            layoutParams.height = sendIconHeight
            button_chat_send.layoutParams = layoutParams
            button_chat_send.setImageDrawable(chatSendDrawable)
            button_chat_send.background = chatSendBackgroundDrawable
            button_chat_send.setPadding(
                chatSendPaddingLeft,
                chatSendPaddingTop,
                chatSendPaddingRight,
                chatSendPaddingBottom
            )
            button_chat_send.setColorFilter(
                sendImageTintColor,
                android.graphics.PorterDuff.Mode.MULTIPLY
            )
            initEmptyView()
        }
        callback.addView(edittext_chat_message)

        swipeToRefresh.setOnRefreshListener {
            if (viewModel?.chatLoaded == true) {
                viewModel?.loadPreviousMessages()
                hidePopUpReactionPanel()
            } else
                swipeToRefresh.isRefreshing = false
        }
    }

    private fun initEmptyView() {
        emptyChatBackgroundView?.let {
            if (chatdisplayBack.childCount == 1) {
                val layoutParam = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                layoutParam.gravity = Gravity.CENTER
                chatdisplayBack.addView(it, layoutParam)
            }
            it.visibility = View.GONE
        }
    }

    /**
     * unix timestamp is passed as param
     * returns the formatted string to display
     */
    open fun formatMessageDateTime(messageTimeStamp: Long?): String {
        if (messageTimeStamp == null || messageTimeStamp == 0L) {
            return ""
        }
        val dateTime = Date()
        dateTime.time = messageTimeStamp
        return DEFAULT_CHAT_MESSAGE_DATE_TIIME_FROMATTER.format(dateTime)
    }

    /**
     * chat session is loaded through this
     */
    fun setSession(session: LiveLikeChatSession) {
        if (this.session === session) return // setting it multiple times same view with same session have a weird behaviour will debug later.
        hideGamification()
        this.session = session.apply {
            (session as ChatSession).analyticsServiceStream.latest()
                ?.trackOrientationChange(resources.configuration.orientation == 1)
        }

        viewModel?.apply {
            chatAdapter.showLinks = enableChatMessageURLs
            chatMessageUrlPatterns?.let {
                if (it.isNotEmpty())
                    chatAdapter.linksRegex = it.toRegex()
            }
            chatAdapter.currentChatReactionPopUpViewPos = -1
            chatAdapter.chatViewThemeAttribute = chatAttribute
            chatAdapter.messageTimeFormatter = { time ->
                formatMessageDateTime(time)
            }
            initStickerKeyboard(sticker_keyboard, this)
            refreshWithDeletedMessage()
            setDataSource(chatAdapter)
            if (chatLoaded)
                checkEmptyChat()
            eventStream.subscribe(javaClass.simpleName) {
                logDebug { "Chat event stream : $it" }
                when (it) {
                    ChatViewModel.EVENT_NEW_MESSAGE -> {
                        // Auto scroll if user is looking at the latest messages
                        autoScroll = true
                        checkEmptyChat()
                        if (viewModel?.isLastItemVisible == true && !swipeToRefresh.isRefreshing && chatAdapter.isReactionPopUpShowing()
                            .not()
                        ) {
                            snapToLive()
                        } else if (chatAdapter.isReactionPopUpShowing() || viewModel?.isLastItemVisible == false) {
                            showSnapToLive()
                        }
                    }
                    ChatViewModel.EVENT_LOADING_COMPLETE -> {
                        uiScope.launch {
                            // Add delay to avoid UI glitch when recycler view is loading
                            delay(500)
                            hideLoadingSpinner()
                            checkEmptyChat()
                            if (!swipeToRefresh.isRefreshing)
                                snapToLive()
                            swipeToRefresh.isRefreshing = false
                            eventStream.onNext(null)
                        }
                    }
                    ChatViewModel.EVENT_LOADING_STARTED -> {
                        uiScope.launch {
                            hideKeyboard(KeyboardHideReason.EXPLICIT_CALL)
                            hideStickerKeyboard(KeyboardHideReason.EXPLICIT_CALL)
                            initEmptyView()
                            delay(400)
                            showLoadingSpinner()
                        }
                    }
                    ChatViewModel.EVENT_MESSAGE_CANNOT_SEND -> {
                        uiScope.launch {
                            AlertDialog.Builder(context).apply {
                                setMessage(context.getString(R.string.send_message_failed_access_denied))
                                setPositiveButton(context.getString(R.string.livelike_chat_report_message_confirm)) { _, _ ->
                                }
                                create()
                            }.show()
                        }
                    }
                }
            }
            userStream.subscribe(javaClass.simpleName) {
                currentUser = it
                it?.let {
                    uiScope.launch {
                        user_profile_tv.visibility = View.VISIBLE
                        user_profile_tv.text = it.nickname
                    }
                }
            }
            programRepository?.programGamificationProfileStream?.subscribe(javaClass.simpleName) {
                it?.let { programRank ->
                    if (programRank.newPoints == 0 || pointView.visibility == View.GONE) {
                        pointView.showPoints(programRank.points)
                        wouldShowBadge(programRank)
                        showUserRank(programRank)
                    } else if (programRank.points == programRank.newPoints) {
                        pointView.apply {
                            postDelayed(
                                {
                                    startAnimationFromTop(programRank.points)
                                    showUserRank(programRank)
                                },
                                6300
                            )
                        }
                    } else {
                        pointView.apply {
                            postDelayed(
                                {
                                    startAnimationFromTop(programRank.points)
                                    showUserRank(programRank)
                                },
                                1000
                            )
                        }
                    }
                }
            }
            animationEventsStream?.subscribe(javaClass.simpleName) {
                if (it == ViewAnimationEvents.BADGE_COLLECTED) {
                    programRepository?.programGamificationProfileStream?.latest()
                        ?.let { programGamificationProfile ->
                            wouldShowBadge(programGamificationProfile, true)
                        }
                }
            }

            chatAdapter.checkListIsAtTop = lambda@{
                val lm: LinearLayoutManager = chatdisplay.layoutManager as LinearLayoutManager
                if (lm.findFirstVisibleItemPosition() == it) {
                    return@lambda true
                }
                return@lambda false
            }

            edittext_chat_message.addTextChangedListener(object : TextWatcher {
                var containsImage = false
                override fun afterTextChanged(s: Editable?) {
                    val matcher = s.toString().findImages()
                    if (matcher.find()) {
                        containsImage = true
                        replaceWithImages(
                            s as Spannable,
                            this@ChatView.context,
                            callback,
                            true
                        )
                        // cleanup before the image
                        if (matcher.start() > 0) edittext_chat_message.text?.delete(
                            0,
                            matcher.start()
                        )

                        // cleanup after the image
                        if (matcher.end() < s.length) edittext_chat_message.text?.delete(
                            matcher.end(),
                            s.length
                        )
                        // Move to end of line
                        edittext_chat_message.setSelection(edittext_chat_message.text?.length ?: 0)
                        if (edittext_chat_message.text?.isNotEmpty() == true)
                            wouldUpdateChatInputAccessibiltyFocus(100)
                    } else if (containsImage) {
                        containsImage = false
                        s?.length?.let { edittext_chat_message.text?.delete(0, it) }
                    } else {
                        containsImage = false
                        stickerPackRepository?.let { stickerPackRepository ->
                            replaceWithStickers(
                                s as Spannable,
                                this@ChatView.context,
                                stickerPackRepository,
                                edittext_chat_message, null
                            )
                        }
                    }
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) = Unit

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) = Unit
            })

            /**
             * stickers are loaded when emoji btn is clicked
             * if sticker keyboard is already visible, then this image changes to qwerty button
             * and on click of qwerty button normal keyboard opens
             * and in case normal keyboard is already visible then this changes to emoji button
             */
            button_emoji.setOnClickListener {
                hidePopUpReactionPanel()
                if (sticker_keyboard.visibility == View.GONE) {
                    showStickerKeyboard()
                } else {
                    hideStickerKeyboard(KeyboardHideReason.CHANGING_KEYBOARD_TYPE)
                    showKeyboard()
                }
            }
            chatdisplay.addOnLayoutChangeListener(layoutChangeListener)
        }
    }

    /**
     * checks if any chats are available
     * if not/ empty list this shows a empty chat background
     */
    private fun checkEmptyChat() {
        emptyChatBackgroundView?.let {
            it.visibility = if ((viewModel?.messageList?.size ?: 0) == 0)
                View.VISIBLE
            else
                View.GONE
        }
    }

    /**
     * stickers keyboard initialization process
     */
    private fun initStickerKeyboard(
        stickerKeyboardView: StickerKeyboardView,
        chatViewModel: ChatViewModel
    ) {
        stickerKeyboardView.initTheme(chatAttribute)
        chatViewModel.stickerPackRepositoryStream.subscribe(this@ChatView) { stickerPackRepository ->
            uiScope.launch {
                stickerPackRepository?.let {
                    stickerKeyboardView.setProgram(stickerPackRepository) {
                        if (it.isNullOrEmpty()) {
                            button_emoji?.visibility = View.GONE
                            sticker_keyboard?.visibility = View.GONE
                        } else {
                            button_emoji?.visibility = View.VISIBLE
                        }
                        viewModel?.chatAdapter?.notifyDataSetChanged()
                    }
                    // used to pass the shortcode to the keyboard
                    stickerKeyboardView.setOnClickListener(object : FragmentClickListener {
                        override fun onClick(sticker: Sticker) {
                            val textToInsert = ":${sticker.shortcode}:"
                            val start = max(edittext_chat_message.selectionStart, 0)
                            val end = max(edittext_chat_message.selectionEnd, 0)
                            if (edittext_chat_message.text!!.length + textToInsert.length < 250) {
                                // replace selected text or start where the cursor is
                                edittext_chat_message.text?.replace(
                                    min(start, end), max(start, end),
                                    textToInsert, 0, textToInsert.length
                                )
                            }
                        }
                    })
                }
            }
        }
    }

    private fun wouldShowBadge(programRank: ProgramGamificationProfile, animate: Boolean = false) {
        var currentBadge = programRank.newBadges?.maxOrNull()
        if (currentBadge == null) {
            currentBadge = programRank.currentBadge
        }
        currentBadge?.let {
            gamification_badge_iv.visibility = View.VISIBLE
            gamification_badge_iv.loadImage(it.imageFile, dpToPx(14))
            if (animate) {
                gamification_badge_iv.buildScaleAnimator(0f, 1f, 1000).start()
            }
        }
    }

    private fun hideGamification() {
        pointView?.visibility = View.GONE
        rank_label?.visibility = View.GONE
        rank_value?.visibility = View.GONE
        gamification_badge_iv?.visibility = View.GONE
    }

    private fun showUserRank(programGamificationProfile: ProgramGamificationProfile) {
        if (programGamificationProfile.points > 0) {
            rank_label.visibility = View.VISIBLE
            rank_value.visibility = View.VISIBLE
            uiScope.async {
                delay(1000)
                rank_value.text = "#${programGamificationProfile.rank}"
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthDp = AndroidResource.pxToDp(width)
        if (widthDp < CHAT_MINIMUM_SIZE_DP && widthDp != 0) {
            logError { "[CONFIG ERROR] Current ChatView Width is $widthDp, it must be more than 292dp or won't display on the screen." }
            setMeasuredDimension(0, 0)
            return
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    /**
     * Hides keyboard on clicking outside of edittext
     */
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        val v = context.scanForActivity()?.currentFocus
        if (v != null &&
            (ev?.action == MotionEvent.ACTION_UP || ev?.action == MotionEvent.ACTION_MOVE) &&
            (v is EditText || v is ChatView) &&
            !v.javaClass.name.startsWith("android.webkit.")
        ) {
            val scrcoords = IntArray(2)
            v.getLocationOnScreen(scrcoords)
            val x = ev.rawX + v.left - scrcoords[0]
            val y = ev.rawY + v.top - scrcoords[1]
            var outsideStickerKeyboardBound =
                (v.bottom - sticker_keyboard.height - button_chat_send.height - button_emoji.height)
            if (button_chat_send.height == 0) {
                outsideStickerKeyboardBound -= chatAttribute.sendIconHeight
            }
            // Added check for image_height greater than 0 so bound position for touch should be above the send icon
            if (!edittext_chat_message.isTouching) {
                if (y < v.top || y > v.bottom || (y < outsideStickerKeyboardBound)) {
                    hideStickerKeyboard(KeyboardHideReason.TAP_OUTSIDE)
                    hideKeyboard(KeyboardHideReason.TAP_OUTSIDE)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun hidePopUpReactionPanel() {
        viewModel?.chatAdapter?.currentChatReactionPopUpViewPos?.let {
            (chatdisplay.findViewHolderForAdapterPosition(it) as? ChatRecyclerAdapter.ViewHolder)?.hideFloatingUI()
        }
    }

    // private var isLastItemVisible = true
    private var autoScroll = false

    /**
     *  Sets the data source for this view.
     *  @param chatAdapter ChatAdapter used for creating this view.
     */
    private fun setDataSource(chatAdapter: ChatRecyclerAdapter) {
        chatdisplay.let { rv ->
            rv.adapter = chatAdapter
            val lm = rv.layoutManager as LinearLayoutManager
            lm.recycleChildrenOnDetach = true
            rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(
                    rv: RecyclerView,
                    dx: Int,
                    dy: Int
                ) {
                    hidePopUpReactionPanel()
                    val totalItemCount = lm.itemCount
                    val lastVisible = lm.findLastVisibleItemPosition()
                    val endHasBeenReached = lastVisible + 5 >= totalItemCount
                    if (!autoScroll)
                        viewModel?.isLastItemVisible =
                            if (totalItemCount > 0 && endHasBeenReached) {
                                hideSnapToLive()
                                true
                            } else {
                                showSnapToLive()
                                false
                            }
                    if (endHasBeenReached) {
                        autoScroll = false
                    }
                }
            })
        }

        snap_live.setOnClickListener {
            hidePopUpReactionPanel()
            autoScroll = true
            snapToLive()
        }

        button_chat_send.let { buttonChat ->
            buttonChat.setOnClickListener { sendMessageNow() }

            if (edittext_chat_message.text.isNullOrEmpty()) {
                buttonChat.visibility = View.GONE
                buttonChat.isEnabled = false
            } else {
                buttonChat.isEnabled = true
                buttonChat.visibility = View.VISIBLE
            }

            edittext_chat_message.apply {
                addTextChangedListener(object : TextWatcher {
                    var previousText: CharSequence = ""
                    override fun beforeTextChanged(
                        s: CharSequence,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                        previousText = s
                    }

                    override fun onTextChanged(
                        s: CharSequence,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                    }

                    override fun afterTextChanged(s: Editable) {
                        if (s.isNotEmpty()) {
                            buttonChat.isEnabled = true
                            buttonChat.visibility = View.VISIBLE
                        } else {
                            buttonChat.isEnabled = false
                            buttonChat.visibility = View.GONE
                        }
                    }
                })

                setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        hidePopUpReactionPanel()
                        (session as? ChatSession)?.analyticsServiceStream?.latest()
                            ?.trackKeyboardOpen(KeyboardType.STANDARD)
                        hideStickerKeyboard(KeyboardHideReason.CHANGING_KEYBOARD_TYPE)
                        viewModel?.chatAdapter?.isKeyboardOpen = true
                    }
                }
            }
        }
    }

    /**
     * used for hiding sticker keyboard / sticker view
     **/
    private fun hideStickerKeyboard(reason: KeyboardHideReason) {
        logDebug { "HideSticker Keyboard: $reason" }
        chatAttribute.apply {
            button_emoji.setImageDrawable(chatStickerSendDrawable)
        }

        findViewById<StickerKeyboardView>(R.id.sticker_keyboard)?.apply {
            //            if (visibility == View.VISIBLE) {
//                session?.analyticService?.trackKeyboardClose(KeyboardType.STICKER, reason)
//            }
            visibility = View.GONE
        }
    }

    /**
     * used for showing sticker keyboard / sticker view
     **/
    private fun showStickerKeyboard() {
        uiScope.launch {
            hideKeyboard(KeyboardHideReason.MESSAGE_SENT)
            (session as? ChatSession)?.analyticsServiceStream?.latest()
                ?.trackKeyboardOpen(KeyboardType.STICKER)
            delay(200) // delay to make sure the keyboard is hidden
            findViewById<StickerKeyboardView>(R.id.sticker_keyboard)?.visibility = View.VISIBLE

            chatAttribute.apply {
                button_emoji.setImageDrawable(chatStickerKeyboardSendDrawable)
            }
            viewModel?.chatAdapter?.isKeyboardOpen = true
        }
    }

    private fun showLoadingSpinner() {
        loadingSpinner.visibility = View.VISIBLE
        chatInput.visibility = View.GONE
        chatdisplay.visibility = View.GONE
        snap_live.visibility = View.GONE
    }

    private fun hideLoadingSpinner() {
        loadingSpinner.visibility = View.GONE
        if (isChatInputVisible) {
            chatInput.visibility = View.VISIBLE
        }
        chatdisplay.visibility = View.VISIBLE
        wouldUpdateChatInputAccessibiltyFocus()
    }

    private fun wouldUpdateChatInputAccessibiltyFocus(time: Long = 500) {
        chatInput.postDelayed(
            {
                edittext_chat_message.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
            },
            time
        )
    }

    /**
     * this is used to hide default keyboard
     **/
    private fun hideKeyboard(reason: KeyboardHideReason) {
        logDebug { "Hide Keyboard : $reason" }
        val inputManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(
            edittext_chat_message.windowToken,
            0
        )

//        session?.analyticService?.trackKeyboardClose(KeyboardType.STANDARD, reason)
        setBackButtonInterceptor(this)
    }

    /**
     * this is used to show default keyboard
     **/
    private fun showKeyboard() {
        edittext_chat_message.requestFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm!!.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
        viewModel?.chatAdapter?.isKeyboardOpen = true
    }

    /**
     * use this to listen messages sent from this view
     **/
    var sentMessageListener: ((message: LiveLikeChatMessage) -> Unit)? = null

    /**
     * Use this function to hide any soft and sticker keyboards over the view.
     **/
    fun dismissKeyboard() {
        hideKeyboard(KeyboardHideReason.EXPLICIT_CALL)
        hideStickerKeyboard(KeyboardHideReason.EXPLICIT_CALL)
    }

    /**
     * This function is used to send message, that user enters
     **/
    private fun sendMessageNow() {
        if (edittext_chat_message.text.isNullOrBlank()) {
            // Do nothing if the message is blank or empty
            return
        }
        val timeData = session?.getPlayheadTime() ?: EpochTime(0)

        // TODO all this can be moved to view model easily
        ChatMessage(
            PubnubChatEventType.MESSAGE_CREATED,
            viewModel?.currentChatRoom?.channels?.chat?.get(CHAT_PROVIDER) ?: "",
            edittext_chat_message.text.toString().trim(),
            "",
            currentUser?.id ?: "empty-id",
            currentUser?.nickname ?: "John Doe",
            session?.avatarUrl,
            isFromMe = true,
            image_width = 100,
            image_height = 100,
            timeStamp = timeData.timeSinceEpochInMs.toString()
        ).let {
            sentMessageListener?.invoke(it.toLiveLikeChatMessage())
            viewModel?.apply {
                displayChatMessage(it)
                val hasExternalImage = (it.message?.findImages()?.countMatches() ?: 0) > 0
                if (hasExternalImage) {
                    uploadAndPostImage(context, it, timeData)
                } else {
                    chatListener?.onChatMessageSend(it, timeData)
                }
                edittext_chat_message.setText("")
                snapToLive()
                viewModel?.currentChatRoom?.id?.let { id ->
                    analyticsService.trackMessageSent(
                        it.id,
                        it.message,
                        hasExternalImage,
                        id
                    )
                }
            }
        }
    }

    /**
     * used for hiding the Snap to live button
     * snap to love is mainly responsible for showing user the latest message
     * if user is already at the latest message,then usually this icon remain hidden
     **/
    private fun hideSnapToLive() {
        logDebug { "Chat hide Snap to Live: $showingSnapToLive" }
        if (!showingSnapToLive)
            return
        showingSnapToLive = false
        snap_live.visibility = View.GONE
        animateSnapToLiveButton()
    }

    /**
     * used for showing the Snap to Live button
     **/
    private fun showSnapToLive() {
        logDebug { "Chat show Snap to Live: $showingSnapToLive" }
        if (showingSnapToLive)
            return
        showingSnapToLive = true
        snap_live.visibility = View.VISIBLE
        animateSnapToLiveButton()
    }

    private fun animateSnapToLiveButton() {
        snapToLiveAnimation?.cancel()

        val translateAnimation = ObjectAnimator.ofFloat(
            snap_live,
            "translationY",
            if (showingSnapToLive) 0f else dpToPx(if (displayUserProfile) SNAP_TO_LIVE_ANIMATION_DESTINATION else 10).toFloat()
        )
        translateAnimation?.duration = SNAP_TO_LIVE_ANIMATION_DURATION.toLong()
        val alphaAnimation =
            ObjectAnimator.ofFloat(snap_live, "alpha", if (showingSnapToLive) 1f else 0f)
        alphaAnimation.duration = (SNAP_TO_LIVE_ALPHA_ANIMATION_DURATION).toLong()
        alphaAnimation.addListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(animation: Animator) {
                snap_live.visibility = if (showingSnapToLive) View.VISIBLE else View.GONE
            }

            override fun onAnimationStart(animation: Animator) {
                snap_live.visibility = if (showingSnapToLive) View.GONE else View.VISIBLE
            }

            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })

        snapToLiveAnimation = AnimatorSet()
        snapToLiveAnimation?.play(translateAnimation)?.with(alphaAnimation)
        snapToLiveAnimation?.start()
    }

    private fun snapToLive() {
        chatdisplay?.let { rv ->
            hideSnapToLive()
            viewModel?.messageList?.size?.let {
                val lm = rv.layoutManager as LinearLayoutManager
                val lastVisiblePosition = lm.itemCount - lm.findLastVisibleItemPosition()
                if (lastVisiblePosition < SMOOTH_SCROLL_MESSAGE_COUNT_LIMIT) {
                    chatdisplay.postDelayed(
                        {
                            rv.smoothScrollToPosition(it)
                        },
                        200
                    )
                } else {
                    chatdisplay.postDelayed(
                        {
                            rv.scrollToPosition(it - 1)
                        },
                        200
                    )
                }
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        wouldUpdateChatInputAccessibiltyFocus()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // added to dismiss popup reaction panel on fragment replace
        chatdisplay.removeOnLayoutChangeListener(layoutChangeListener)
        viewModel?.chatAdapter?.chatPopUpView?.dismiss()
    }

    /**
     *This should generally be called to cleanup at time of onDestroy of activity and onDestroyView of fragment
     **/
    fun clearSession() {
        viewModel?.apply {
            eventStream.unsubscribe(javaClass.simpleName)
            userStream.unsubscribe(javaClass.simpleName)
            programRepository?.programGamificationProfileStream?.unsubscribe(javaClass.simpleName)
            stickerPackRepositoryStream.unsubscribe(this@ChatView)
            chatAdapter.checkListIsAtTop = null
            chatAdapter.mRecyclerView = null
            chatAdapter.messageTimeFormatter = null
        }
        chatdisplay.adapter = null
        targetDrawables.clear()
        targetByteArrays.clear()
    }

    companion object {
        const val SNAP_TO_LIVE_ANIMATION_DURATION = 400F
        const val SNAP_TO_LIVE_ALPHA_ANIMATION_DURATION = 320F
        const val SNAP_TO_LIVE_ANIMATION_DESTINATION = 50
        private const val CHAT_MINIMUM_SIZE_DP = 292
        private const val SMOOTH_SCROLL_MESSAGE_COUNT_LIMIT = 100
    }
}
