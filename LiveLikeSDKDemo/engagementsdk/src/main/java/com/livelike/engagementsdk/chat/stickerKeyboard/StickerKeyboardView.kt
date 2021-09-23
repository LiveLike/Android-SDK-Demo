package com.livelike.engagementsdk.chat.stickerKeyboard

import android.animation.LayoutTransition
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.chat.ChatViewThemeAttributes
import com.livelike.engagementsdk.chat.utils.liveLikeSharedPrefs.filterRecentStickers
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.logDebug
import kotlinx.android.synthetic.main.livelike_sticker_keyboard_pager.view.pager
import kotlinx.android.synthetic.main.livelike_sticker_keyboard_pager.view.pager_tab

class StickerKeyboardView(context: Context?, attributes: AttributeSet? = null) :
    ConstraintLayout(context!!, attributes) {
    private var viewModel: StickerKeyboardViewModel? = null
    private var chatViewThemeAttributes: ChatViewThemeAttributes? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.livelike_sticker_keyboard_pager, this, true)
        layoutTransition = LayoutTransition()
    }

    fun initTheme(themeAttributes: ChatViewThemeAttributes) {
        chatViewThemeAttributes = themeAttributes
        themeAttributes.apply {
            pager.background = stickerBackground
            pager_tab.background = stickerTabBackground
            pager_tab.setSelectedTabIndicatorColor(stickerSelectedTabIndicatorColor)
        }
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        if (visibility == View.VISIBLE) {
            val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(changedView.windowToken, 0)
        }
        super.onVisibilityChanged(changedView, visibility)
    }

    private fun createTabItemView(stickerPack: StickerPack? = null): View {
        val imageView = ImageView(context)
        imageView.contentDescription =
            stickerPack?.name ?: context.getString(R.string.recent_sticker)
        chatViewThemeAttributes?.let {
            if (stickerPack?.file == null)
                imageView.setColorFilter(
                    it.stickerSelectedTabIndicatorColor,
                    android.graphics.PorterDuff.Mode.MULTIPLY
                )
        }
        imageView.layoutParams = ViewGroup.LayoutParams(
            AndroidResource.dpToPx(24),
            AndroidResource.dpToPx(24)
        )
        Glide.with(this).load((stickerPack?.file) ?: R.drawable.keyboard_ic_recent).into(imageView)
        return imageView
    }

    fun setProgram(
        stickerPackRepository: StickerPackRepository,
        onLoaded: ((List<StickerPack>?) -> Unit)? = null
    ) {
        viewModel = StickerKeyboardViewModel(stickerPackRepository)
        viewModel?.stickerPacks?.subscribe(javaClass) {
            onLoaded?.invoke(it)
            it?.let { stickerPacks ->
                logDebug { "sticker pack: ${stickerPacks.size}" }
                filterRecentStickers(stickerPackRepository.programId, stickerPacks)
                val stickerCollectionPagerAdapter = StickerCollectionAdapter(
                    stickerPacks,
                    stickerPackRepository.programId,
                    emptyRecentTextColor = chatViewThemeAttributes?.stickerRecentEmptyTextColor
                        ?: Color.WHITE
                ) { s -> listener?.onClick(s) }
                pager_tab.removeAllTabs()
                pager.layoutManager =
                    LinearLayoutManager(
                        context,
                        LinearLayoutManager.HORIZONTAL,
                        false
                    )
                pager.adapter = stickerCollectionPagerAdapter
                val pageListener = object : TabLayout.TabLayoutOnPageChangeListener(pager_tab) {
                    override fun onPageScrollStateChanged(state: Int) {
                        super.onPageScrollStateChanged(state)
                    }

                    override fun onPageScrolled(
                        position: Int,
                        positionOffset: Float,
                        positionOffsetPixels: Int
                    ) {
                        super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                    }

                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)
                    }
                }
                RVPagerSnapHelperListenable().attachToRecyclerView(
                    pager,
                    object : RVPagerStateListener {
                        override fun onPageScroll(pagesState: List<VisiblePageState>) {
                            for (pos in pagesState.indices) {
                                val state = pagesState[pos]
                                var diff = (1.0f - state.distanceToSettled)
                                if (diff < 0.0f) {
                                    diff = 0.0f
                                } else if (diff > 1.0f) {
                                    diff = 1.0f
                                }
                                if (pos == 0) {
                                    pageListener.onPageScrolled(
                                        state.index,
                                        diff,
                                        state.distanceToSettledPixels
                                    )
                                }
                            }
                        }

                        override fun onScrollStateChanged(state: RVPageScrollState) {
                            pageListener.onPageScrollStateChanged(
                                when (state) {
                                    RVPageScrollState.Idle -> {
                                        pager_tab.getTabAt(pager_tab.selectedTabPosition)?.select()
                                        ViewPager.SCROLL_STATE_IDLE
                                    }
                                    RVPageScrollState.Dragging -> ViewPager.SCROLL_STATE_DRAGGING
                                    RVPageScrollState.Settling -> ViewPager.SCROLL_STATE_SETTLING
                                }
                            )
                        }

                        override fun onPageSelected(index: Int) {
                            pageListener.onPageSelected(index)
                        }
                    }
                )

                val listener = object : TabLayout.BaseOnTabSelectedListener<TabLayout.Tab> {
                    override fun onTabReselected(p0: TabLayout.Tab?) {
                    }

                    override fun onTabUnselected(p0: TabLayout.Tab?) {
                    }

                    override fun onTabSelected(p0: TabLayout.Tab?) {
                        logDebug { "Sticker Tab Selected :${p0?.position}" }
                        p0?.let {
                            pager.smoothScrollToPosition(p0.position)
                        }
                    }
                }
                pager_tab.addOnTabSelectedListener(listener)
                for (i in 0 until stickerCollectionPagerAdapter.itemCount) {
                    val tab = pager_tab.newTab()
                    if (i == StickerCollectionAdapter.RECENT_STICKERS_POSITION) {
                        tab.customView = createTabItemView()
                    } else {
                        tab.customView = createTabItemView(stickerPacks[i - 1])
                    }
                    pager_tab.addTab(tab)
                }
            }
            viewModel?.preload(context.applicationContext)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewModel?.stickerPacks?.unsubscribe(javaClass)
    }

    private var listener: FragmentClickListener? = null

    fun setOnClickListener(listener: FragmentClickListener) {
        this.listener = listener
    }
}
