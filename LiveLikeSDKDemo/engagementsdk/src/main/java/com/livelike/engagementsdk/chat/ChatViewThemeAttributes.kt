package com.livelike.engagementsdk.chat

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.core.utils.AndroidResource

class ChatViewThemeAttributes {
    fun initAttributes(context: Context, typedArray: TypedArray?) {
        typedArray?.apply {
            chatAvatarCircle = getBoolean(R.styleable.ChatView_chatAvatarCircle, true)
            showStickerSend = getBoolean(R.styleable.ChatView_showStickerSend, true)
            showMessageDateTime = getBoolean(R.styleable.ChatView_showMessageTime, true)
            chatNickNameColor = getColor(
                R.styleable.ChatView_usernameColor,
                ContextCompat.getColor(context, R.color.livelike_openChatNicknameMe)
            )
            chatOtherNickNameColor = getColor(
                R.styleable.ChatView_otherUsernameColor,
                ContextCompat.getColor(context, R.color.livelike_openChatNicknameOther)
            )
            chatMessageColor = getColor(
                R.styleable.ChatView_messageColor,
                ContextCompat.getColor(
                    context,
                    R.color.livelike_default_chat_cell_message_color
                )
            )
            rankValueTextColor = getColor(
                R.styleable.ChatView_rankValueTextColor,
                Color.WHITE
            )

            sendImageTintColor = getColor(
                R.styleable.ChatView_sendIconTintColor,
                ContextCompat.getColor(context, android.R.color.white)
            )
            sendStickerTintColor = getColor(
                R.styleable.ChatView_stickerIconTintColor,
                ContextCompat.getColor(context, android.R.color.white)
            )

            chatAvatarGravity =
                getInt(R.styleable.ChatView_chatAvatarGravity, Gravity.NO_GRAVITY)

            val colorBubbleValue = TypedValue()
            getValue(R.styleable.ChatView_chatBubbleBackground, colorBubbleValue)

            chatBubbleBackgroundRes = when {
                colorBubbleValue.type == TypedValue.TYPE_REFERENCE || colorBubbleValue.type == TypedValue.TYPE_STRING -> getResourceId(
                    R.styleable.ChatView_chatBubbleBackground,
                    R.drawable.ic_chat_message_bubble_rounded_rectangle
                )
                colorBubbleValue.type == TypedValue.TYPE_NULL -> R.drawable.ic_chat_message_bubble_rounded_rectangle
                colorBubbleValue.type >= TypedValue.TYPE_FIRST_COLOR_INT && colorBubbleValue.type <= TypedValue.TYPE_LAST_COLOR_INT -> colorBubbleValue.data
                else -> R.drawable.ic_chat_message_bubble_rounded_rectangle
            }

            val colorHighlightedBubbleValue = TypedValue()
            getValue(
                R.styleable.ChatView_chatReactionMessageBubbleHighlightedBackground,
                colorHighlightedBubbleValue
            )

            chatReactionMessageBubbleHighlightedBackground = when {
                colorHighlightedBubbleValue.type == TypedValue.TYPE_REFERENCE || colorHighlightedBubbleValue.type == TypedValue.TYPE_STRING -> getResourceId(
                    R.styleable.ChatView_chatReactionMessageBubbleHighlightedBackground,
                    R.drawable.ic_chat_message_highlighted_bubble_rounded_rectangle
                )
                colorHighlightedBubbleValue.type == TypedValue.TYPE_NULL -> R.drawable.ic_chat_message_highlighted_bubble_rounded_rectangle
                colorHighlightedBubbleValue.type >= TypedValue.TYPE_FIRST_COLOR_INT && colorHighlightedBubbleValue.type <= TypedValue.TYPE_LAST_COLOR_INT -> colorHighlightedBubbleValue.data
                else -> R.drawable.ic_chat_message_highlighted_bubble_rounded_rectangle
            }

            val colorBackValue = TypedValue()
            getValue(R.styleable.ChatView_chatBackground, colorBackValue)

            chatBackgroundRes = when {
                colorBackValue.type == TypedValue.TYPE_REFERENCE || colorBackValue.type == TypedValue.TYPE_STRING -> getResourceId(
                    R.styleable.ChatView_chatBackground,
                    android.R.color.transparent
                )
                colorBackValue.type == TypedValue.TYPE_NULL -> null
                colorBackValue.type >= TypedValue.TYPE_FIRST_COLOR_INT && colorBackValue.type <= TypedValue.TYPE_LAST_COLOR_INT -> colorBackValue.data
                else -> null
            }

            val colorHighlightedBackValue = TypedValue()
            getValue(
                R.styleable.ChatView_chatReactionMessageBackHighlightedBackground,
                colorHighlightedBackValue
            )

            chatReactionMessageBackHighlightedBackground = when {
                colorHighlightedBackValue.type == TypedValue.TYPE_REFERENCE || colorHighlightedBackValue.type == TypedValue.TYPE_STRING -> getResourceId(
                    R.styleable.ChatView_chatReactionMessageBackHighlightedBackground,
                    android.R.color.transparent
                )
                colorHighlightedBackValue.type == TypedValue.TYPE_NULL -> null
                colorHighlightedBackValue.type >= TypedValue.TYPE_FIRST_COLOR_INT && colorHighlightedBackValue.type <= TypedValue.TYPE_LAST_COLOR_INT -> colorHighlightedBackValue.data
                else -> null
            }

            val sendDrawable = TypedValue()
            getValue(R.styleable.ChatView_chatSendDrawable, sendDrawable)

            chatSendDrawable = when (sendDrawable.type) {
                TypedValue.TYPE_REFERENCE, TypedValue.TYPE_STRING -> ContextCompat.getDrawable(
                    context,
                    getResourceId(
                        R.styleable.ChatView_chatSendDrawable,
                        R.drawable.ic_chat_send
                    )
                )
                else -> ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_chat_send
                )
            }

            val sendStickerDrawable = TypedValue()
            getValue(R.styleable.ChatView_chatStickerSendDrawable, sendStickerDrawable)

            chatStickerSendDrawable = when (sendStickerDrawable.type) {
                TypedValue.TYPE_REFERENCE, TypedValue.TYPE_STRING -> ContextCompat.getDrawable(
                    context,
                    getResourceId(
                        R.styleable.ChatView_chatStickerSendDrawable,
                        R.drawable.ic_chat_emoji_ios_category_smileysandpeople
                    )
                )
                else -> ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_chat_emoji_ios_category_smileysandpeople
                )
            }

            val sendKeyboardStickerDrawable = TypedValue()
            getValue(
                R.styleable.ChatView_chatStickerKeyboardSendDrawable,
                sendKeyboardStickerDrawable
            )

            chatStickerKeyboardSendDrawable = when (sendKeyboardStickerDrawable.type) {
                TypedValue.TYPE_REFERENCE, TypedValue.TYPE_STRING -> ContextCompat.getDrawable(
                    context,
                    getResourceId(
                        R.styleable.ChatView_chatStickerKeyboardSendDrawable,
                        R.drawable.ic_chat_keyboard
                    )
                )
                else -> ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_chat_keyboard
                )
            }

            val userPicDrawable = TypedValue()
            getValue(R.styleable.ChatView_userPicDrawable, userPicDrawable)

            chatUserPicDrawable = when (userPicDrawable.type) {
                TypedValue.TYPE_REFERENCE, TypedValue.TYPE_STRING -> ContextCompat.getDrawable(
                    context,
                    getResourceId(
                        R.styleable.ChatView_userPicDrawable,
                        R.drawable.default_avatar
                    )
                )
                else -> ContextCompat.getDrawable(
                    context,
                    R.drawable.default_avatar
                )
            }

            val chatSendBackValue = TypedValue()
            getValue(R.styleable.ChatView_chatSendBackground, chatSendBackValue)

            chatSendBackgroundDrawable = when (chatSendBackValue.type) {
                TypedValue.TYPE_REFERENCE, TypedValue.TYPE_STRING -> ContextCompat.getDrawable(
                    context,
                    getResourceId(
                        R.styleable.ChatView_chatSendBackground,
                        android.R.color.transparent
                    )
                )
                TypedValue.TYPE_NULL -> ContextCompat.getDrawable(
                    context,
                    android.R.color.transparent
                )
                else -> ColorDrawable(chatSendBackValue.data)
            }

            val colorReactionValue = TypedValue()
            getValue(R.styleable.ChatView_chatReactionBackground, colorReactionValue)

            chatReactionBackgroundRes = when (colorReactionValue.type) {
                TypedValue.TYPE_REFERENCE, TypedValue.TYPE_STRING -> ContextCompat.getDrawable(
                    context,
                    getResourceId(
                        R.styleable.ChatView_chatReactionBackground,
                        android.R.color.transparent
                    )
                )
                TypedValue.TYPE_NULL -> ContextCompat.getDrawable(
                    context,
                    android.R.color.transparent
                )
                else -> ColorDrawable(colorReactionValue.data)
            }

            val colorViewValue = TypedValue()
            getValue(R.styleable.ChatView_chatViewBackground, colorViewValue)

            chatViewBackgroundRes = when (colorViewValue.type) {
                TypedValue.TYPE_REFERENCE, TypedValue.TYPE_STRING -> ContextCompat.getDrawable(
                    context,
                    getResourceId(
                        R.styleable.ChatView_chatViewBackground,
                        android.R.color.transparent
                    )
                )
                TypedValue.TYPE_NULL -> ColorDrawable(Color.TRANSPARENT)
                else -> ColorDrawable(colorViewValue.data)
            }

            val colorChatDisplayValue = TypedValue()
            getValue(R.styleable.ChatView_chatDisplayBackground, colorChatDisplayValue)

            chatDisplayBackgroundRes = when (colorChatDisplayValue.type) {
                TypedValue.TYPE_REFERENCE, TypedValue.TYPE_STRING -> ContextCompat.getDrawable(
                    context,
                    getResourceId(
                        R.styleable.ChatView_chatDisplayBackground,
                        android.R.color.transparent
                    )
                )
                TypedValue.TYPE_NULL -> ColorDrawable(Color.TRANSPARENT)
                else -> ColorDrawable(colorChatDisplayValue.data)
            }

            val colorInputBackgroundValue = TypedValue()
            getValue(R.styleable.ChatView_chatInputBackground, colorInputBackgroundValue)

            chatInputBackgroundRes = when (colorInputBackgroundValue.type) {
                TypedValue.TYPE_REFERENCE, TypedValue.TYPE_STRING -> ContextCompat.getDrawable(
                    context,
                    getResourceId(
                        R.styleable.ChatView_chatInputBackground,
                        R.drawable.ic_chat_input
                    )
                )
                TypedValue.TYPE_NULL -> ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_chat_input
                )
                else -> ColorDrawable(colorInputBackgroundValue.data)
            }

            val colorInputViewBackgroundValue = TypedValue()
            getValue(
                R.styleable.ChatView_chatInputViewBackground,
                colorInputViewBackgroundValue
            )

            chatInputViewBackgroundRes = when (colorInputViewBackgroundValue.type) {
                TypedValue.TYPE_REFERENCE, TypedValue.TYPE_STRING -> ContextCompat.getDrawable(
                    context,
                    getResourceId(
                        R.styleable.ChatView_chatInputViewBackground,
                        android.R.color.transparent
                    )
                )
                TypedValue.TYPE_NULL -> ColorDrawable(
                    ContextCompat.getColor(context, android.R.color.transparent)
                )
                else -> ColorDrawable(colorInputViewBackgroundValue.data)
            }

            chatInputTextColor = getColor(
                R.styleable.ChatView_chatInputTextColor,
                ContextCompat.getColor(context, R.color.livelike_chat_input_text_color)
            )
            chatInputHintTextColor = getColor(
                R.styleable.ChatView_chatInputTextHintColor,
                ContextCompat.getColor(context, R.color.livelike_chat_input_text_color)
            )

            chatBubbleWidth = getLayoutDimension(
                R.styleable.ChatView_chatBubbleWidth,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            chatBackgroundWidth = getLayoutDimension(
                R.styleable.ChatView_chatBackgroundWidth,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )

            sendIconHeight = getLayoutDimension(
                R.styleable.ChatView_sendButtonHeight,
                AndroidResource.dpToPx(40)
            )
            sendIconWidth = getLayoutDimension(
                R.styleable.ChatView_sendButtonWidth,
                AndroidResource.dpToPx(56)
            )

            chatInputTextSize = getDimensionPixelSize(
                R.styleable.ChatView_chatInputTextSize,
                resources.getDimensionPixelSize(R.dimen.livelike_default_chat_input_text_size)
            )
            chatReactionX =
                getDimensionPixelSize(
                    R.styleable.ChatView_chatReactionXPosition,
                    AndroidResource.dpToPx(120)
                )
            chatReactionY = getDimensionPixelSize(
                R.styleable.ChatView_chatReactionYPosition,
                AndroidResource.dpToPx(-5)
            )
            chatReactionElevation = getDimensionPixelSize(
                R.styleable.ChatView_chatReactionElevation,
                AndroidResource.dpToPx(0)
            ).toFloat()
            chatReactionRadius = getDimensionPixelSize(
                R.styleable.ChatView_chatReactionRadius,
                AndroidResource.dpToPx(17)
            ).toFloat()
            chatSelectedReactionRadius = getDimensionPixelSize(
                R.styleable.ChatView_chatSelectedReactionRadius,
                AndroidResource.dpToPx(17)
            ).toFloat()
            chatReactionPadding =
                getDimensionPixelSize(
                    R.styleable.ChatView_chatReactionPadding,
                    AndroidResource.dpToPx(3)
                )
            chatAvatarHeight =
                getDimensionPixelSize(
                    R.styleable.ChatView_chatAvatarHeight,
                    AndroidResource.dpToPx(32)
                )
            chatAvatarWidth =
                getDimensionPixelSize(
                    R.styleable.ChatView_chatAvatarWidth,
                    AndroidResource.dpToPx(32)
                )
            chatAvatarRadius =
                getDimensionPixelSize(
                    R.styleable.ChatView_chatAvatarRadius,
                    AndroidResource.dpToPx(20)
                )
            chatAvatarMarginLeft =
                getDimensionPixelSize(
                    R.styleable.ChatView_chatAvatarMarginLeft,
                    AndroidResource.dpToPx(13)
                )
            chatAvatarMarginRight =
                getDimensionPixelSize(
                    R.styleable.ChatView_chatAvatarMarginRight,
                    AndroidResource.dpToPx(3)
                )
            chatAvatarMarginBottom = getDimensionPixelSize(
                R.styleable.ChatView_chatAvatarMarginBottom,
                AndroidResource.dpToPx(5)
            )
            chatAvatarMarginTop =
                getDimensionPixelSize(
                    R.styleable.ChatView_chatAvatarMarginTop,
                    AndroidResource.dpToPx(0)
                )

            chatReactionPanelColor = getColor(
                R.styleable.ChatView_chatReactionPanelColor,
                Color.WHITE
            )

            chatReactionPanelCountColor = getColor(
                R.styleable.ChatView_chatReactionPanelCountColor,
                ContextCompat.getColor(context, android.R.color.black)
            )

            chatReactionDisplayCountColor = getColor(
                R.styleable.ChatView_chatReactionDisplayCountColor,
                ContextCompat.getColor(context, android.R.color.white)
            )

            chatReactionFlagTintColor = getColor(
                R.styleable.ChatView_chatReactionFlagTintColor,
                ContextCompat.getColor(context, android.R.color.black)
            )

            chatBubblePaddingLeft = getDimensionPixelOffset(
                R.styleable.ChatView_chatBubblePaddingLeft,
                resources.getDimension(R.dimen.livelike_default_chat_cell_padding_left).toInt()
            )
            chatBubblePaddingRight = getDimensionPixelOffset(
                R.styleable.ChatView_chatBubblePaddingRight,
                resources.getDimension(R.dimen.livelike_default_chat_cell_padding_right).toInt()
            )
            chatBubblePaddingTop = getDimensionPixelOffset(
                R.styleable.ChatView_chatBubblePaddingTop,
                resources.getDimension(R.dimen.livelike_default_chat_cell_padding_top).toInt()
            )
            chatBubblePaddingBottom = getDimensionPixelOffset(
                R.styleable.ChatView_chatBubblePaddingBottom,
                resources.getDimension(R.dimen.livelike_default_chat_cell_padding_bottom).toInt()
            )

            chatBubbleMarginLeft = getDimensionPixelOffset(
                R.styleable.ChatView_chatBubbleMarginLeft, 5
            )
            chatBubbleMarginRight = getDimensionPixelOffset(
                R.styleable.ChatView_chatBubbleMarginRight, 25
            )
            chatBubbleMarginTop = getDimensionPixelOffset(
                R.styleable.ChatView_chatBubbleMarginTop, 0
            )
            chatBubbleMarginBottom = getDimensionPixelOffset(
                R.styleable.ChatView_chatBubbleMarginBottom, 0
            )

            chatSendPaddingLeft = getDimensionPixelOffset(
                R.styleable.ChatView_chatSendButtonPaddingLeft,
                AndroidResource.dpToPx(10)
            )
            chatSendPaddingRight = getDimensionPixelOffset(
                R.styleable.ChatView_chatSendButtonPaddingRight,
                AndroidResource.dpToPx(10)
            )
            chatSendPaddingTop = getDimensionPixelOffset(
                R.styleable.ChatView_chatSendButtonPaddingTop,
                AndroidResource.dpToPx(6)
            )
            chatSendPaddingBottom = getDimensionPixelOffset(
                R.styleable.ChatView_chatSendButtonPaddingBottom,
                AndroidResource.dpToPx(6)
            )

            chatMarginLeft = getDimensionPixelOffset(
                R.styleable.ChatView_chatMarginLeft,
                AndroidResource.dpToPx(8)
            )
            chatMarginRight = getDimensionPixelOffset(
                R.styleable.ChatView_chatMarginRight,
                AndroidResource.dpToPx(8)
            )
            chatMarginTop =
                getDimensionPixelOffset(
                    R.styleable.ChatView_chatMarginTop,
                    AndroidResource.dpToPx(4)
                )
            chatMarginBottom = getDimensionPixelOffset(
                R.styleable.ChatView_chatMarginBottom,
                AndroidResource.dpToPx(4)
            )
            chatMarginBottom = getDimensionPixelOffset(
                R.styleable.ChatView_chatMarginBottom,
                AndroidResource.dpToPx(4)
            )

            val stickerBackgroundValue = TypedValue()
            getValue(
                R.styleable.ChatView_stickerBackground,
                stickerBackgroundValue
            )
            stickerBackground = when (stickerBackgroundValue.type) {
                TypedValue.TYPE_REFERENCE, TypedValue.TYPE_STRING -> ContextCompat.getDrawable(
                    context,
                    getResourceId(
                        R.styleable.ChatView_stickerBackground,
                        android.R.color.transparent
                    )
                )
                TypedValue.TYPE_NULL -> ColorDrawable(
                    ContextCompat.getColor(context, android.R.color.transparent)
                )
                else -> ColorDrawable(stickerBackgroundValue.data)
            }

            val stickerTabBackgroundValue = TypedValue()
            getValue(
                R.styleable.ChatView_stickerTabBackground,
                stickerTabBackgroundValue
            )
            stickerTabBackground = when (stickerTabBackgroundValue.type) {
                TypedValue.TYPE_REFERENCE, TypedValue.TYPE_STRING -> ContextCompat.getDrawable(
                    context,
                    getResourceId(
                        R.styleable.ChatView_stickerTabBackground,
                        android.R.color.transparent
                    )
                )
                TypedValue.TYPE_NULL -> ColorDrawable(
                    ContextCompat.getColor(context, android.R.color.transparent)
                )
                else -> ColorDrawable(stickerTabBackgroundValue.data)
            }
            stickerSelectedTabIndicatorColor = getColor(
                R.styleable.ChatView_stickerSelectedTabIndicatorColor,
                ContextCompat.getColor(context, android.R.color.white)
            )
            stickerRecentEmptyTextColor = getColor(
                R.styleable.ChatView_stickerRecentEmptyTextColor,
                ContextCompat.getColor(context, R.color.livelike_sticker_recent_empty_text_color)
            )
            chatMessageTopBorderColor = getColor(
                R.styleable.ChatView_chatMessageTopBorderColor,
                ContextCompat.getColor(context, android.R.color.transparent)
            )
            chatMessageBottomBorderColor = getColor(
                R.styleable.ChatView_chatMessageBottomBorderColor,
                ContextCompat.getColor(context, android.R.color.transparent)
            )
            chatMessageTopBorderHeight =
                getDimensionPixelSize(
                    R.styleable.ChatView_chatMessageTopBorderHeight,
                    AndroidResource.dpToPx(0)
                )
            chatMessageBottomBorderHeight =
                getDimensionPixelSize(
                    R.styleable.ChatView_chatMessageBottomBorderHeight,
                    AndroidResource.dpToPx(0)
                )
            chatReactionHintEnable = getBoolean(R.styleable.ChatView_reaction_hint_enable, true)
            chatReactionHintIcon = getResourceId(
                R.styleable.ChatView_reaction_icon,
                R.drawable.ic_chat_reaction_default
            )
            chatReactionIconsMarginLeft = getDimensionPixelOffset(
                R.styleable.ChatView_reaction_icons_margin_left,
                AndroidResource.dpToPx(0)
            )
            chatReactionIconsMarginBottom = getDimensionPixelOffset(
                R.styleable.ChatView_reaction_icons_margin_bottom,
                AndroidResource.dpToPx(0)
            )
            chatReactionIconsMarginRight = getDimensionPixelOffset(
                R.styleable.ChatView_reaction_icons_margin_right,
                AndroidResource.dpToPx(3)
            )
            chatReactionIconsMarginTop = getDimensionPixelOffset(
                R.styleable.ChatView_reaction_icons_margin_top,
                AndroidResource.dpToPx(5)
            )
            chatReactionCountMarginLeft = getDimensionPixelOffset(
                R.styleable.ChatView_reaction_count_margin_left,
                AndroidResource.dpToPx(0)
            )
            chatReactionCountMarginBottom = getDimensionPixelOffset(
                R.styleable.ChatView_reaction_count_margin_bottom,
                AndroidResource.dpToPx(0)
            )
            chatReactionCountMarginRight = getDimensionPixelOffset(
                R.styleable.ChatView_reaction_count_margin_right,
                AndroidResource.dpToPx(13)
            )
            chatReactionCountMarginTop = getDimensionPixelOffset(
                R.styleable.ChatView_reaction_count_margin_top,
                AndroidResource.dpToPx(4)
            )
            chatReactionIconsPositionAtBottom =
                getBoolean(R.styleable.ChatView_reaction_icon_position_bottom, false)
            chatReactionCountPositionAtBottom =
                getBoolean(R.styleable.ChatView_reaction_count_icons_position_bottom, false)
            chatReactionIconsFactor =
                getFloat(R.styleable.ChatView_reaction_icons_gap_factor, 1.2f)
            chatReactionModerationFlagVisible =
                getBoolean(R.styleable.ChatView_chatReactionModerationFlagVisible, true)
            chatUserNameTextStyle =
                getInt(R.styleable.ChatView_chatUserNameTextStyle, Typeface.BOLD)
            chatUserNameCustomFontPath = getString(R.styleable.ChatView_chatUserNameCustomFontPath)
            chatUserNameTextAllCaps =
                getBoolean(R.styleable.ChatView_chatUserNameTextAllCaps, false)
            chatUserNameTextSize = getDimension(
                R.styleable.ChatView_chatUserNameTextSize,
                AndroidResource.spToPx(12.0f)
            )
            chatMessageCustomFontPath = getString(R.styleable.ChatView_chatMessageCustomFontPath)
            chatMessageTextStyle = getInt(R.styleable.ChatView_chatMessageTextStyle, 0)
            chatMessageTextSize = getDimension(
                R.styleable.ChatView_chatMessageTextSize,
                AndroidResource.spToPx(12.0f)
            )
            chatMessageTimeCustomFontPath =
                getString(R.styleable.ChatView_chatMessageTimeCustomFontPath)
            chatMessageTimeTextSize = getDimension(
                R.styleable.ChatView_chatMessageTimeTextSize,
                AndroidResource.spToPx(10.0f)
            )
            chatMessageTimeTextStyle = getInt(R.styleable.ChatView_chatMessageTimeTextStyle, 0)
            chatMessageTimeTextAllCaps =
                getBoolean(R.styleable.ChatView_chatMessageTimeTextAllCaps, false)
            chatMessageTimeTextColor = getColor(
                R.styleable.ChatView_chatMessageTimeTextColor,
                ContextCompat.getColor(context, R.color.livelike_chatMessage_timestamp_text_color)
            )
            chatReactionDisplayCountTextStyle =
                getInt(R.styleable.ChatView_chatReactionDisplayCountTextStyle, 0)
            chatReactionDisplayCountCustomFontPath =
                getString(R.styleable.ChatView_chatReactionDisplayCountCustomFontPath)
            chatReactionPanelCountCustomFontPath =
                getString(R.styleable.ChatView_chatReactionPanelCountCustomFontPath)
            chatReactionDisplayCountTextSize = getDimension(
                R.styleable.ChatView_chatReactionDisplayCountTextSize,
                AndroidResource.spToPx(11f)
            )
            chatReactionDisplaySize = getDimensionPixelSize(
                R.styleable.ChatView_chatReactionDisplaySize,
                AndroidResource.dpToPx(12)
            )
            chatReactionPanelGravity =
                getInt(R.styleable.ChatView_chatReactionPanelGravity, Gravity.CENTER or Gravity.TOP)
            chatReactionPanelCountVisibleIfZero =
                getBoolean(R.styleable.ChatView_chatReactionPanelCountVisibleIfZero, true)
            chatMessageTimeTextLetterSpacing =
                getFloat(R.styleable.ChatView_chatMessageTimeTextLetterSpacing, 0.0f)
            chatMessageTextLetterSpacing =
                getFloat(R.styleable.ChatView_chatMessageTextLetterSpacing, 0.0f)
            chatUserNameTextLetterSpacing =
                getFloat(R.styleable.ChatView_chatUserNameTextLetterSpacing, 0.0f)
            chatMessageLinkTextColor = getColor(
                R.styleable.ChatView_chatMessageLinkTextColor,
                ContextCompat.getColor(context, R.color.livelike_chatMessage_link_text_color)
            )
        }
    }

    var showMessageDateTime: Boolean = true
    var chatBubblePaddingLeft: Int = 0
    var chatBubblePaddingRight: Int = 0
    var chatBubblePaddingTop: Int = 0
    var chatBubblePaddingBottom: Int = 0
    var chatSendPaddingLeft: Int = AndroidResource.dpToPx(10)
    var chatSendPaddingRight: Int = AndroidResource.dpToPx(10)
    var chatSendPaddingTop: Int = AndroidResource.dpToPx(6)
    var chatSendPaddingBottom: Int = AndroidResource.dpToPx(6)
    var chatMarginLeft: Int = 0
    var chatMarginRight: Int = 0
    var chatMarginTop: Int = 0
    var chatMarginBottom: Int = 0
    var chatBubbleMarginLeft: Int = AndroidResource.dpToPx(13)
    var chatBubbleMarginRight: Int = AndroidResource.dpToPx(25)
    var chatBubbleMarginTop: Int = 0
    var chatBubbleMarginBottom: Int = 0
    var chatBubbleWidth: Int = LinearLayout.LayoutParams.WRAP_CONTENT
    var chatBackgroundWidth: Int = ConstraintLayout.LayoutParams.WRAP_CONTENT
    var sendIconWidth: Int = 0
    var sendIconHeight: Int = 0
    var chatInputTextSize: Int = 0
    var chatBubbleBackgroundRes: Int? = R.drawable.ic_chat_message_bubble_rounded_rectangle
    var chatBackgroundRes: Int? = null
    var chatViewBackgroundRes: Drawable? = null
    var chatInputBackgroundRes: Drawable? = null
    var chatInputViewBackgroundRes: Drawable? = null
    var chatDisplayBackgroundRes: Drawable? = null
    var chatSendDrawable: Drawable? = null
    var chatStickerSendDrawable: Drawable? = null
    var chatStickerKeyboardSendDrawable: Drawable? = null
    var chatUserPicDrawable: Drawable? = null
    var chatSendBackgroundDrawable: Drawable? = null
    var chatMessageColor: Int = Color.TRANSPARENT
    var sendImageTintColor: Int = Color.WHITE
    var sendStickerTintColor: Int = Color.WHITE
    var rankValueTextColor: Int = Color.WHITE
    var chatInputTextColor: Int = Color.TRANSPARENT
    var chatInputHintTextColor: Int = Color.TRANSPARENT
    var chatOtherNickNameColor: Int = Color.TRANSPARENT
    var chatNickNameColor: Int = Color.TRANSPARENT
    var chatReactionBackgroundRes: Drawable? = null
    var chatReactionMessageBubbleHighlightedBackground: Int? =
        R.drawable.ic_chat_message_highlighted_bubble_rounded_rectangle
    var chatReactionMessageBackHighlightedBackground: Int? = null
    var chatReactionPanelColor: Int = Color.WHITE
    var chatReactionPanelCountColor: Int = Color.BLACK
    var chatReactionDisplayCountColor: Int = Color.WHITE
    var chatReactionFlagTintColor: Int = Color.BLACK
    var chatReactionX: Int = AndroidResource.dpToPx(120)
    var chatReactionY: Int = AndroidResource.dpToPx(-5)
    var chatReactionElevation: Float = 4f
    var chatReactionRadius: Float = AndroidResource.dpToPx(20).toFloat()
    var chatSelectedReactionRadius: Float = AndroidResource.dpToPx(20).toFloat()
    var chatReactionPadding: Int = 0
    var chatAvatarMarginRight: Int = AndroidResource.dpToPx(3)
    var chatAvatarMarginBottom: Int = AndroidResource.dpToPx(5)
    var chatAvatarMarginLeft: Int = AndroidResource.dpToPx(5)
    var chatAvatarMarginTop: Int = AndroidResource.dpToPx(0)
    var chatAvatarRadius: Int = AndroidResource.dpToPx(20)
    var chatAvatarCircle: Boolean = true
    var showStickerSend: Boolean = true
    var chatAvatarWidth: Int = AndroidResource.dpToPx(32)
    var chatAvatarHeight: Int = AndroidResource.dpToPx(32)
    var chatAvatarGravity: Int = Gravity.NO_GRAVITY
    var stickerBackground: Drawable? = null
    var stickerTabBackground: Drawable? = null
    var stickerSelectedTabIndicatorColor: Int = Color.WHITE
    var stickerRecentEmptyTextColor: Int = Color.WHITE
    var chatMessageTopBorderColor: Int = Color.TRANSPARENT
    var chatMessageBottomBorderColor: Int = Color.TRANSPARENT
    var chatMessageTopBorderHeight: Int = 0
    var chatMessageBottomBorderHeight: Int = 0
    var chatReactionHintEnable: Boolean = false
    var chatReactionHintIcon: Int = R.drawable.ic_chat_reaction_default
    var chatReactionIconsMarginLeft: Int = AndroidResource.dpToPx(0)
    var chatReactionIconsMarginTop: Int = AndroidResource.dpToPx(5)
    var chatReactionIconsMarginRight: Int = AndroidResource.dpToPx(3)
    var chatReactionIconsMarginBottom: Int = AndroidResource.dpToPx(0)
    var chatReactionCountMarginLeft: Int = AndroidResource.dpToPx(0)
    var chatReactionCountMarginTop: Int = AndroidResource.dpToPx(4)
    var chatReactionCountMarginRight: Int = AndroidResource.dpToPx(13)
    var chatReactionCountMarginBottom: Int = AndroidResource.dpToPx(0)
    var chatReactionIconsPositionAtBottom: Boolean = false
    var chatReactionCountPositionAtBottom: Boolean = false
    var chatReactionIconsFactor: Float = 1.2f
    var chatReactionModerationFlagVisible: Boolean = true
    var chatUserNameTextStyle: Int = Typeface.BOLD
    var chatUserNameCustomFontPath: String? = null
    var chatUserNameTextAllCaps: Boolean = false
    var chatUserNameTextSize: Float = AndroidResource.spToPx(12.0f)
    var chatMessageCustomFontPath: String? = null
    var chatMessageTextStyle: Int = 0
    var chatMessageTextSize: Float = AndroidResource.spToPx(12.0f)
    var chatMessageTimeCustomFontPath: String? = null
    var chatMessageTimeTextSize: Float = AndroidResource.spToPx(10.0f)
    var chatMessageTimeTextStyle: Int = 0
    var chatMessageTimeTextAllCaps: Boolean = false
    var chatMessageTimeTextColor: Int = Color.WHITE
    var chatMessageTimeTextLetterSpacing: Float = 0.0f
    var chatUserNameTextLetterSpacing: Float = 0.0f
    var chatMessageTextLetterSpacing: Float = 0.0f
    var chatReactionDisplayCountTextStyle: Int = 0
    var chatReactionDisplayCountCustomFontPath: String? = null
    var chatReactionPanelCountCustomFontPath: String? = null
    var chatReactionDisplayCountTextSize: Float = AndroidResource.spToPx(11f)
    var chatReactionDisplaySize: Int = AndroidResource.dpToPx(12)
    var chatReactionPanelGravity: Int = Gravity.CENTER or Gravity.TOP
    var chatReactionPanelCountVisibleIfZero: Boolean = true
    var chatMessageLinkTextColor: Int = Color.BLUE
}
