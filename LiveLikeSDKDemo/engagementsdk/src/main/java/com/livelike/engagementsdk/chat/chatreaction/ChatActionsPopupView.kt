package com.livelike.engagementsdk.chat.chatreaction

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.chat.ChatMessageReaction
import com.livelike.engagementsdk.chat.ChatViewThemeAttributes
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.widget.view.loadImage
import kotlinx.android.synthetic.main.popup_chat_reaction.view.chat_reaction_background_card
import kotlinx.android.synthetic.main.popup_chat_reaction.view.moderation_flag
import kotlinx.android.synthetic.main.popup_chat_reaction.view.moderation_flag_lay

/**
 * Chat reactions and Chat moderation actions view that will popup when use long press chat
 */
internal class ChatActionsPopupView(
    val context: Context,
    private val chatReactionRepository: ChatReactionRepository?,
    flagClick: View.OnClickListener,
    hideFloatingUi: () -> Unit,
    isOwnMessage: Boolean,
    var userReaction: ChatMessageReaction? = null,
    var emojiCountMap: MutableMap<String, Int>? = null,
    private val chatViewThemeAttributes: ChatViewThemeAttributes,
    val selectReactionListener: SelectReactionListener? = null,
    val isPublichat: Boolean
) : PopupWindow(context) {

    init {
        chatViewThemeAttributes.apply {
            contentView = LayoutInflater.from(context).inflate(R.layout.popup_chat_reaction, null)
            contentView.chat_reaction_background_card.apply {
                setCardBackgroundColor(chatReactionPanelColor)
                cardElevation = chatReactionElevation
                radius = chatReactionRadius
                setContentPadding(
                    chatReactionPadding,
                    chatReactionPadding,
                    chatReactionPadding,
                    chatReactionPadding
                )
            }

            if (chatReactionModerationFlagVisible) {
                if (!isOwnMessage) {
                    contentView.moderation_flag_lay.apply {
                        visibility = View.VISIBLE
                        setOnClickListener {
                            dismiss()
                            flagClick.onClick(it)
                        }
                        radius = chatReactionRadius
                        setCardBackgroundColor(chatReactionPanelColor)
                    }
                    contentView.moderation_flag.setColorFilter(
                        chatReactionFlagTintColor
                    )
                }
            } else {
                contentView.moderation_flag_lay.visibility = View.GONE
            }

            setOnDismissListener(hideFloatingUi)
            isOutsideTouchable = false
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        initReactions()
    }

    private fun formattedReactionCount(count: Int): String {
        return if (count < 99)
            "$count"
        else
            "99+"
    }

    fun updatePopView(
        emojiCountMap: MutableMap<String, Int>? = null,
        userReaction: ChatMessageReaction? = null
    ) {
        this.userReaction = userReaction
        this.emojiCountMap = emojiCountMap
        initReactions()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initReactions() {
        val reactionsBox =
            contentView.findViewById<LinearLayout>(R.id.reaction_panel_interaction_box)
        reactionsBox.removeAllViews()
        chatReactionRepository?.reactionList?.forEach { reaction ->
            val cardView = CardView(context)
            cardView.cardElevation = 0f
            cardView.setContentPadding(5, 5, 8, 5)
            cardView.setCardBackgroundColor(Color.TRANSPARENT)
            val relativeLayout = RelativeLayout(context)
            val countView = TextView(context)
            val imageView = ImageView(context)
            imageView.id = View.generateViewId()
            imageView.isFocusable = true
            imageView.contentDescription = reaction.name
            imageView.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            imageView.loadImage(
                reaction.file,
                context.resources.getDimensionPixelSize(R.dimen.livelike_chat_reaction_size)
            )

            userReaction?.let {
                if (it.emojiId == reaction.id) {
                    cardView.radius = chatViewThemeAttributes.chatSelectedReactionRadius
                    cardView.setCardBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            R.color.livelike_chat_reaction_selected_background_color
                        )
                    )
                }
            }
            // On Touch we are scaling and descaling the reaction imageview to show bounce feature
            imageView.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.animate().scaleX(1.2f).scaleY(1.2f).setDuration(50).start()
                        return@setOnTouchListener true
                    }
                    MotionEvent.ACTION_UP -> {
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(50).start()
                        selectReactionListener?.let {
                            if (userReaction != null) {
                                if (userReaction?.emojiId == reaction.id) {
                                    it.onSelectReaction(null) // No selection
                                } else
                                    it.onSelectReaction(reaction)
                            } else
                                it.onSelectReaction(reaction)
                            dismiss()
                        }
                        return@setOnTouchListener true
                    }
                }
                return@setOnTouchListener false
            }

            imageView.scaleType = ImageView.ScaleType.CENTER

            val count = emojiCountMap?.get(reaction.id) ?: 0
            countView.apply {
                gravity = Gravity.RIGHT
                text = formattedReactionCount(count)
                setTextColor(chatViewThemeAttributes.chatReactionPanelCountColor)
                if (chatViewThemeAttributes.chatReactionPanelCountCustomFontPath != null) {
                    try {
                        val typeFace =
                            Typeface.createFromAsset(
                                context.assets,
                                chatViewThemeAttributes.chatReactionPanelCountCustomFontPath
                            )
                        setTypeface(typeFace, Typeface.BOLD)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        setTypeface(null, Typeface.BOLD)
                    }
                } else {
                    setTypeface(null, Typeface.BOLD)
                }
                setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    context.resources.getDimension(R.dimen.livelike_chat_reaction_popup_text_size)
                )
                visibility = if (!chatViewThemeAttributes.chatReactionPanelCountVisibleIfZero) {
                    if (count > 0) {
                        View.VISIBLE
                    } else {
                        View.INVISIBLE
                    }
                } else {
                    View.VISIBLE
                }
            }
            relativeLayout.addView(
                imageView,
                RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    addRule(RelativeLayout.CENTER_IN_PARENT)
                }
            )
            relativeLayout.addView(
                countView,
                RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    addRule(RelativeLayout.ALIGN_TOP, imageView.id)
                    addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                }
            )
            cardView.addView(relativeLayout)
            reactionsBox.addView(
                cardView,
                LinearLayout.LayoutParams(AndroidResource.dpToPx(35), AndroidResource.dpToPx(35))
            )
        }
        contentView.chat_reaction_background_card.visibility =
            if ((chatReactionRepository?.reactionList?.size ?: 0) > 0) {
                View.VISIBLE
            } else {
                View.INVISIBLE
            }
        contentView.chat_reaction_background_card.postDelayed(
            {
                contentView.chat_reaction_background_card.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
            },
            500
        )
    }
}

internal interface SelectReactionListener {
    fun onSelectReaction(reaction: Reaction?)
}
