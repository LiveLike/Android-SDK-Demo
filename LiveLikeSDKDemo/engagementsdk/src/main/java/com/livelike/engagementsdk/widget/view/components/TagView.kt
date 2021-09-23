package com.livelike.engagementsdk.widget.view.components

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.widget.ViewStyleProps
import kotlinx.android.synthetic.main.atom_widget_tag_view.view.tagTextView



class TagView (context: Context, attr: AttributeSet) : ConstraintLayout(context, attr) {
    var tag: String = ""
        set(value) {
            field = value
            tagTextView.text = value
        }
    var componentTheme: ViewStyleProps? = null
        set(value) {
            field = value
            value?.padding?.let { padding ->
                setPadding(
                    AndroidResource.webPxToDevicePx(padding[0].toInt()),
                    AndroidResource.webPxToDevicePx(padding[1].toInt()),
                    AndroidResource.webPxToDevicePx(padding[2].toInt()),
                    AndroidResource.webPxToDevicePx(padding[3].toInt())
                )
            }
        }

    init {
        inflate(context, R.layout.atom_widget_tag_view, this)
    }
}
