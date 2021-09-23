@file:Suppress("UNCHECKED_CAST")

package com.livelike.engagementsdk.chat.stickerKeyboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.annotation.Px
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.chat.utils.liveLikeSharedPrefs.addRecentSticker
import com.livelike.engagementsdk.chat.utils.liveLikeSharedPrefs.getRecentStickers
import com.livelike.engagementsdk.core.utils.logDebug
import kotlinx.android.synthetic.main.livelike_sticker_keyboard_item.view.itemImage
import kotlinx.android.synthetic.main.livelike_sticker_keyboard_rv.view.empty_recent_text
import kotlinx.android.synthetic.main.livelike_sticker_keyboard_rv.view.rvStickers

class StickerCollectionAdapter(
    private val stickerPacks: List<StickerPack>,
    val programId: String,
    private val emptyRecentTextColor: Int = R.color.livelike_sticker_recent_empty_text_color,
    private val onClickCallback: (Sticker) -> Unit
) : RecyclerView.Adapter<StickerCollectionViewHolder>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, index: Int): StickerCollectionViewHolder {
        return StickerCollectionViewHolder(
            LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.livelike_sticker_keyboard_rv, viewGroup, false),
            emptyRecentTextColor
        ) { sticker ->
            notifyDataSetChanged()
            onClickCallback(sticker)
        }
    }

    override fun getItemCount(): Int {
        return when (stickerPacks.isEmpty()) {
            true -> 0
            else -> stickerPacks.size + 1
        }
    }

    override fun onBindViewHolder(viewHolder: StickerCollectionViewHolder, index: Int) {
        val pack = when (index) {
            RECENT_STICKERS_POSITION -> null
            else -> stickerPacks[index - 1]
        }
        viewHolder.bind(pack, index == RECENT_STICKERS_POSITION, programId)
    }

    companion object {
        const val RECENT_STICKERS_POSITION = 0
    }
}

class StickerCollectionViewHolder(
    itemView: View,
    emptyRecentTextColor: Int,
    var onClickCallback: (Sticker) -> Unit
) : RecyclerView.ViewHolder(itemView) {

    init {
        itemView.rvStickers.layoutManager =
            GridLayoutManager(itemView.context, 6)
        itemView.empty_recent_text.setTextColor(emptyRecentTextColor)
    }

    fun bind(stickerPack: StickerPack?, isRecent: Boolean, programId: String) {
        val adapter = StickerAdapter { sticker -> onClickCallback(sticker) }
        itemView.rvStickers.adapter = adapter
        if (isRecent) {
            val stickers = getRecentStickers(programId)
            logDebug { "Recent Sticker Count: ${stickers.size}" }
            itemView.empty_recent_text?.visibility = if (stickers.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
            adapter.submitList(stickers)
        } else {
            itemView.empty_recent_text?.visibility = View.GONE
            adapter.submitList(stickerPack!!.stickers)
        }
    }
}

interface FragmentClickListener {
    fun onClick(sticker: Sticker)
}

class StickerDiffCallback : DiffUtil.ItemCallback<Sticker>() {
    override fun areItemsTheSame(oldItem: Sticker, newItem: Sticker): Boolean {
        return oldItem.shortcode == newItem.shortcode
    }

    override fun areContentsTheSame(oldItem: Sticker, newItem: Sticker): Boolean {
        return oldItem.shortcode == newItem.shortcode
    }
}

class StickerAdapter(private val onClick: (Sticker) -> Unit) :
    ListAdapter<Sticker, StickerAdapter.StickerViewHolder>(StickerDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StickerViewHolder {
        return StickerViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.livelike_sticker_keyboard_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: StickerViewHolder, position: Int) {
        holder.onBind(getItem(position), onClick)
    }

    class StickerViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        fun onBind(sticker: Sticker, onClick: (Sticker) -> Unit) {
            Glide.with(view).load(sticker.file).diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(view.itemImage)
            view.itemImage.contentDescription = sticker.shortcode
            view.itemImage.setOnClickListener {
                onClick(sticker)
                addRecentSticker(sticker)
            }
        }
    }
}

class PagerSnapScrollListener(
    private val recyclerView: RecyclerView,
    private val externalListener: RVPagerStateListener,
    maxPages: Int
) : RecyclerView.OnScrollListener() {
    var pageStates: MutableList<VisiblePageState> = ArrayList(maxPages)
    var pageStatesPool = List(maxPages) { VisiblePageState(0, recyclerView, 0, 0, 0f) }

    init {
        recyclerView.addOnScrollListener(this)
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager

        val firstPos = layoutManager.findFirstVisibleItemPosition()
        val lastPos = layoutManager.findLastVisibleItemPosition()
        val screenEndX = recyclerView.width
        val midScreen = (screenEndX / 2)

        for (position in firstPos..lastPos) {
            val view = layoutManager.findViewByPosition(position)!!
            val viewWidth = view.measuredWidth
            val viewStartX = view.x
            val viewEndX = viewStartX + viewWidth
            if (viewEndX >= 0 && viewStartX <= screenEndX) {
                val viewHalfWidth = view.measuredWidth / 2f

                val pageState = pageStatesPool[position - firstPos]
                pageState.index = position
                pageState.view = view
                pageState.viewCenterX = (viewStartX + viewWidth / 2f).toInt()
                pageState.distanceToSettledPixels = (pageState.viewCenterX - midScreen)
                pageState.distanceToSettled =
                    (pageState.viewCenterX + viewHalfWidth) / (midScreen + viewHalfWidth)
                pageStates.add(pageState)
            }
        }
        externalListener.onPageScroll(pageStates)

        // Clear this in advance so as to avoid holding refs to views.
        pageStates.clear()
    }

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        externalListener.onScrollStateChanged(statesArray[newState])
    }

    companion object {
        val statesArray =
            listOf(RVPageScrollState.Idle, RVPageScrollState.Dragging, RVPageScrollState.Settling)
    }
}

sealed class RVPageScrollState {
    object Idle : RVPageScrollState()
    object Dragging : RVPageScrollState()
    object Settling : RVPageScrollState()
}

data class VisiblePageState(
    var index: Int,
    var view: View,
    @Px var viewCenterX: Int,
    @Px var distanceToSettledPixels: Int,
    var distanceToSettled: Float
)

interface RVPagerStateListener {
    fun onPageScroll(pagesState: List<VisiblePageState>) {}
    fun onScrollStateChanged(state: RVPageScrollState) {}
    fun onPageSelected(index: Int) {}
}

open class RVPagerSnapHelperListenable(private val maxPages: Int = 3) {
    fun attachToRecyclerView(recyclerView: RecyclerView, listener: RVPagerStateListener) {
        assertRecyclerViewSetup(recyclerView)
        recyclerView.onFlingListener = null
        setUpSnapHelper(recyclerView, listener)
        setUpScrollListener(recyclerView, listener)
    }

    private fun setUpScrollListener(recyclerView: RecyclerView, listener: RVPagerStateListener) =
        PagerSnapScrollListener(recyclerView, listener, maxPages)

    private fun setUpSnapHelper(recyclerView: RecyclerView, listener: RVPagerStateListener) =
        PagerSnapHelperVerbose(recyclerView, listener).attachToRecyclerView(recyclerView)

    private fun assertRecyclerViewSetup(recyclerView: RecyclerView) {
        if (recyclerView.layoutManager !is LinearLayoutManager) {
            throw IllegalArgumentException("RVPagerSnapHelperListenable can only work with a linear layout manager")
        }

        if ((recyclerView.layoutManager as LinearLayoutManager).orientation != LinearLayoutManager.HORIZONTAL) {
            throw IllegalArgumentException("RVPagerSnapHelperListenable can only work with a horizontal orientation")
        }
    }
}

class PagerSnapHelperVerbose(
    private val recyclerView: RecyclerView,
    private val externalListener: RVPagerStateListener
) : PagerSnapHelper(), ViewTreeObserver.OnGlobalLayoutListener {

    private var lastPage = RecyclerView.NO_POSITION

    init {
        recyclerView.viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    override fun onGlobalLayout() {
        val position =
            (recyclerView.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
        if (position != RecyclerView.NO_POSITION) {
            notifyNewPageIfNeeded(position)
            recyclerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
        }
    }

    override fun findSnapView(layoutManager: RecyclerView.LayoutManager?): View? {
        val view = super.findSnapView(layoutManager)
        view?.let {
            notifyNewPageIfNeeded(recyclerView.getChildAdapterPosition(view))
        }
        return view
    }

    override fun findTargetSnapPosition(
        layoutManager: RecyclerView.LayoutManager?,
        velocityX: Int,
        velocityY: Int
    ): Int {
        val position = super.findTargetSnapPosition(layoutManager, velocityX, velocityY)

        if (position < recyclerView.adapter?.itemCount ?: 0) { // Making up for a "bug" in the original snap-helper.
            notifyNewPageIfNeeded(position)
        }
        return position
    }

    private fun notifyNewPageIfNeeded(page: Int?) {
        page?.let {
            if (page != lastPage) {
                this.externalListener.onPageSelected(page)
                lastPage = page
            }
        }
    }
}
