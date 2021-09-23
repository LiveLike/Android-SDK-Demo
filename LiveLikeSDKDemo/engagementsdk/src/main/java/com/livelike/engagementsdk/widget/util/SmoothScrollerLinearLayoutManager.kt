package com.livelike.engagementsdk.widget.util

import android.content.Context
import android.graphics.PointF
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView

class SmoothScrollerLinearLayoutManager : LinearLayoutManager {
    constructor(context: Context) : this(context, VERTICAL, false)
    constructor(context: Context, orientation: Int, reverseValue: Boolean) : super(context, orientation, reverseValue)

    override fun smoothScrollToPosition(recyclerView: RecyclerView?, state: RecyclerView.State?, position: Int) {
        val smoothScroller = TopSnappedSmoothScroller(recyclerView?.context)
        smoothScroller.targetPosition = position
        startSmoothScroll(smoothScroller)
    }

    private class TopSnappedSmoothScroller(context: Context?) : LinearSmoothScroller(context) {
        var mContext = context
        override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
            return SmoothScrollerLinearLayoutManager(mContext as Context)
                .computeScrollVectorForPosition(targetPosition)
        }

        override fun getVerticalSnapPreference(): Int {
            return SNAP_TO_START
        }
    }
}
