package com.livelike.engagementsdk.widget.util

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

// This layout takes up all the space available and disable scrolling
class SpanningLinearLayoutManager(context: Context, val itemMinSize: Int) :
    LinearLayoutManager(context) {

    private val horizontalSpace: Int
        get() = width - paddingRight - paddingLeft

    private val verticalSpace: Int
        get() = height - paddingBottom - paddingTop

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return spanLayoutSize(super.generateDefaultLayoutParams())
    }

    override fun generateLayoutParams(c: Context, attrs: AttributeSet): RecyclerView.LayoutParams {
        return spanLayoutSize(super.generateLayoutParams(c, attrs))
    }

    override fun generateLayoutParams(lp: ViewGroup.LayoutParams): RecyclerView.LayoutParams {
        return spanLayoutSize(super.generateLayoutParams(lp))
    }

    private var itemSize: Int = 0
    private fun spanLayoutSize(layoutParams: RecyclerView.LayoutParams): RecyclerView.LayoutParams {
        if (orientation == HORIZONTAL) {
            itemSize = (horizontalSpace / itemCount.toDouble()).roundToInt()
            if (itemSize > itemMinSize) {
                layoutParams.width = itemSize
            } else {
                layoutParams.width = itemMinSize
            }
        } else if (orientation == VERTICAL) {
            itemSize = (verticalSpace / itemCount.toDouble()).roundToInt()
            if (itemSize > itemMinSize) {
                layoutParams.height = itemSize
            } else {
                layoutParams.height = itemMinSize
            }
        }
        return layoutParams
    }

    override fun canScrollVertically(): Boolean {
        return (itemSize < itemMinSize) && orientation == VERTICAL
    }

    override fun canScrollHorizontally(): Boolean {
        return (itemSize < itemMinSize) && orientation == HORIZONTAL
    }
}
