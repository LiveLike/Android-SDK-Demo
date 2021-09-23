package com.livelike.engagementsdk.chat

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.chat.stickerKeyboard.countMatches
import com.livelike.engagementsdk.chat.stickerKeyboard.findImages

class RichContentEditText : AppCompatEditText {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        setAccessibilityDelegate(object : AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View?,
                info: AccessibilityNodeInfo?
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info?.text?.let { s ->
                    val hasExternalImage = s.toString().findImages().countMatches() > 0
                    info.contentDescription = ""
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        info.hintText = ""
                    }
                    info.text = if (hasExternalImage)
                        context.getString(R.string.image)
                    else
                        s
                }
            }
        })
    }

    override fun onCreateInputConnection(editorInfo: EditorInfo): InputConnection {
        val ic: InputConnection = super.onCreateInputConnection(editorInfo)
        if (allowMediaFromKeyboard) {
            EditorInfoCompat.setContentMimeTypes(
                editorInfo,
                arrayOf("image/*", "image/gif", "image/png")
            )

            val callback =
                InputConnectionCompat.OnCommitContentListener { inputContentInfo, flags, opts ->
                    val lacksPermission = (
                        flags and
                            InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
                        ) != 0
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && lacksPermission) {
                        try {
                            inputContentInfo.requestPermission()
                        } catch (e: Exception) {
                            return@OnCommitContentListener false
                        }
                    }
                    context.contentResolver.openAssetFileDescriptor(
                        inputContentInfo.contentUri,
                        "r"
                    )
                        ?.use {
                            if (it.length > 3_000_000) {
                                return@OnCommitContentListener false
                            }
                        }
                    setText(":${inputContentInfo.contentUri}:")
                    true
                }
            return InputConnectionCompat.createWrapper(ic, editorInfo, callback)
        }
        return ic
    }

    var allowMediaFromKeyboard: Boolean = true
    var isTouching = false

    /**
     * this touch is override to check if the user scrolling the chat list so uneven opening of the keyboard
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        isTouching =
            event?.action == MotionEvent.ACTION_DOWN || event?.action == MotionEvent.ACTION_MOVE
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }
}
