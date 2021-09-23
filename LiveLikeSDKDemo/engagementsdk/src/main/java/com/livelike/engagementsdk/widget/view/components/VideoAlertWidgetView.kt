package com.livelike.engagementsdk.widget.view.components

import android.content.Context
import android.content.Intent
import android.graphics.Outline
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.core.utils.logError
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.WidgetsTheme
import com.livelike.engagementsdk.widget.model.Alert
import com.livelike.engagementsdk.widget.viewModel.BaseViewModel
import com.livelike.engagementsdk.widget.viewModel.VideoWidgetViewModel
import com.livelike.engagementsdk.widget.viewModel.WidgetStates
import kotlinx.android.synthetic.main.video_widget.view.bodyText
import kotlinx.android.synthetic.main.video_widget.view.ic_play
import kotlinx.android.synthetic.main.video_widget.view.ic_sound
import kotlinx.android.synthetic.main.video_widget.view.labelText
import kotlinx.android.synthetic.main.video_widget.view.linkArrow
import kotlinx.android.synthetic.main.video_widget.view.linkBackground
import kotlinx.android.synthetic.main.video_widget.view.linkText
import kotlinx.android.synthetic.main.video_widget.view.mute_tv
import kotlinx.android.synthetic.main.video_widget.view.playbackErrorView
import kotlinx.android.synthetic.main.video_widget.view.playerView
import kotlinx.android.synthetic.main.video_widget.view.progress_bar
import kotlinx.android.synthetic.main.video_widget.view.sound_view
import kotlinx.android.synthetic.main.video_widget.view.thumbnailView
import kotlinx.android.synthetic.main.video_widget.view.widgetContainer

internal class VideoAlertWidgetView : SpecifiedWidgetView {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private var inflated = false
    var viewModel: VideoWidgetViewModel? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isMuted: Boolean = false
    private var playedAtLeastOnce: Boolean = false
    private var stopPosition: Int = 0

    override var dismissFunc: ((action: DismissAction) -> Unit)? =
        {
            viewModel?.dismissWidget(it)
            removeAllViews()
        }

    override var widgetViewModel: BaseViewModel? = null
        set(value) {
            field = value
            viewModel = value as VideoWidgetViewModel
        }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewModel?.data?.subscribe(javaClass) {
            logDebug { "showing the Video WidgetView" }
            it?.let {
                inflate(context, it)
            }
        }
        viewModel?.widgetState?.subscribe(javaClass) { widgetStates ->
            logDebug { "Current State: $widgetStates" }
            widgetStates?.let {
                if (widgetStates == WidgetStates.INTERACTING) {
                    // will only be fired if link is available in alert widget
                    viewModel?.markAsInteractive()
                }
                if (viewModel?.enableDefaultWidgetTransition == true) {
                    defaultStateTransitionManager(widgetStates)
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewModel?.data?.unsubscribe(javaClass)
        viewModel?.widgetState?.unsubscribe(javaClass)
        release()
    }

    override fun moveToNextState() {
        super.moveToNextState()
        if (widgetViewModel?.widgetState?.latest() == WidgetStates.INTERACTING) {
            widgetViewModel?.widgetState?.onNext(WidgetStates.FINISHED)
        } else {
            super.moveToNextState()
        }
    }

    private fun inflate(context: Context, resourceAlert: Alert) {
        if (!inflated) {
            inflated = true
            inflate(context, R.layout.video_widget, this@VideoAlertWidgetView)
        }
        bodyText.text = resourceAlert.text
        labelText.text = resourceAlert.title
        linkText.text = resourceAlert.link_label
        sound_view.visibility = View.GONE
        playbackErrorView.visibility = View.GONE

        if (!resourceAlert.videoUrl.isNullOrEmpty()) {
            setFrameThumbnail(resourceAlert.videoUrl)
        }

        if (!resourceAlert.link_url.isNullOrEmpty()) {
            linkBackground.setOnClickListener {
                openBrowser(context, resourceAlert.link_url)
            }
        } else {
            linkArrow.visibility = View.GONE
            linkBackground.visibility = View.GONE
            linkText.visibility = View.GONE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                setPlayerViewCornersRound(isOnlyBottomCornersToBeRounded = true)
            }
        }

        if (resourceAlert.title.isNullOrEmpty()) {
            labelText.visibility = GONE
            widgetContainer.setBackgroundResource(R.drawable.video_alert_all_rounded_corner)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                setPlayerViewCornersRound(isOnlyBottomCornersToBeRounded = false)
            }
            val params = widgetContainer.layoutParams as ConstraintLayout.LayoutParams
            params.topMargin = AndroidResource.dpToPx(0)
            widgetContainer.requestLayout()
        } else {
            widgetContainer.setBackgroundResource(R.drawable.video_alert_rounded_corner_black_background)
            val params = widgetContainer.layoutParams as ConstraintLayout.LayoutParams
            widgetContainer.layoutParams = params
            widgetContainer.requestLayout()
        }

        if (!resourceAlert.text.isNullOrEmpty()) {
            bodyText.visibility = View.VISIBLE
            bodyText.text = resourceAlert.text
        } else {
            bodyText.visibility = View.GONE
        }

        setOnClickListeners()

        widgetsTheme?.let {
            applyTheme(it)
        }
    }

    override fun applyTheme(theme: WidgetsTheme) {
        super.applyTheme(theme)
        viewModel?.data?.latest()?.let { _ ->
            theme.getThemeLayoutComponent(WidgetType.VIDEO_ALERT)?.let { themeComponent ->
                AndroidResource.updateThemeForView(
                    labelText,
                    themeComponent.title,
                    fontFamilyProvider
                )
                if (themeComponent.header?.background != null) {
                    labelText?.background = AndroidResource.createDrawable(themeComponent.header)
                }
                themeComponent.header?.padding?.let {
                    AndroidResource.setPaddingForView(labelText, themeComponent.header.padding)
                }

                widgetContainer?.background =
                    AndroidResource.createDrawable(themeComponent.body)

                AndroidResource.updateThemeForView(
                    linkText,
                    themeComponent.body,
                    fontFamilyProvider
                )
            }
        }
    }

    /** sets the listeners */
    private fun setOnClickListeners() {
        sound_view.setOnClickListener {
            if (isMuted) {
                unMute()
            } else {
                mute()
            }
        }

        widgetContainer.setOnClickListener {
            if (playerView?.isPlaying == true) {
                pause()
            } else {
                if (stopPosition > 0) { // already running
                    resume()
                } else {
                    play()
                }
            }
        }
    }

    /** sets the video view */
    private fun initializePlayer(videoUrl: String) {
        try {
            val uri = Uri.parse(videoUrl)
            playerView.setVideoURI(uri)
            // playerView.seekTo(stopPosition)
            playerView.requestFocus()
            playerView.start()
            unMute()

            // perform set on prepared listener event on video view
            try {
                playerView.setOnPreparedListener { mp ->
                    // do something when video is ready to play
                    this.mediaPlayer = mp
                    playedAtLeastOnce = true
                    progress_bar.visibility = View.GONE
                    playbackErrorView.visibility = View.GONE
                    sound_view.visibility = VISIBLE
                    ic_sound.visibility = VISIBLE
                }

                playerView.setOnCompletionListener {
                    playerView?.stopPlayback()
                    sound_view.visibility = GONE
                    setFrameThumbnail(videoUrl)
                }

                playerView.setOnErrorListener { _, _, _ ->
                    logError { "Error on playback" }
                    progress_bar.visibility = GONE
                    ic_play.visibility = GONE
                    playerView.visibility = INVISIBLE
                    playbackErrorView.visibility = VISIBLE
                    sound_view.visibility = GONE
                    true
                }
            } catch (e: Exception) {
                progress_bar.visibility = GONE
                playbackErrorView.visibility = VISIBLE
                e.printStackTrace()
            }
        } catch (e: Exception) {
            progress_bar.visibility = GONE
            playbackErrorView.visibility = VISIBLE
            e.printStackTrace()
        }
    }

    /** responsible for playing the video */
    private fun play() {
        progress_bar.visibility = View.VISIBLE
        viewModel?.registerPlayStarted()
        ic_play.visibility = View.GONE
        playbackErrorView.visibility = View.GONE
        thumbnailView.visibility = View.GONE
        playerView.visibility = View.VISIBLE
        viewModel?.data?.latest()?.videoUrl?.let { initializePlayer(it) }
    }

    /** responsible for resuming the video from where it was stopped */
    private fun resume() {
        sound_view.visibility = VISIBLE
        playbackErrorView.visibility = GONE
        progress_bar.visibility = GONE
        ic_play.visibility = GONE
        playerView.seekTo(stopPosition)
        if (playerView.currentPosition == 0) {
            play()
        } else {
            playerView.start()
        }
    }

    /** responsible for stopping the video */
    private fun pause() {
        stopPosition = playerView.currentPosition
        playerView.pause()
        sound_view.visibility = GONE
        ic_play.visibility = View.VISIBLE
        playbackErrorView.visibility = View.GONE
        ic_play.setImageResource(R.drawable.ic_play_button)
    }

    /** responsible for stopping the player and releasing it */
    private fun release() {
        try {
            playedAtLeastOnce = false
            if (playerView != null && playerView.isPlaying) {
                playerView.stopPlayback()
                playerView.seekTo(0)
                stopPosition = 0
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
            }
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    /** checks if the player is paused */
    fun isPaused(): Boolean {
        return !mediaPlayer!!.isPlaying && playedAtLeastOnce
    }

    /** mutes the video */
    private fun mute() {
        try {
            isMuted = true
            mediaPlayer?.setVolume(0f, 0f)
            ic_sound.setImageResource(R.drawable.ic_volume_on)
            mute_tv.text = context.resources.getString(R.string.livelike_unmute_label)
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    /** unmute the video */
    private fun unMute() {
        try {
            isMuted = false
            mediaPlayer?.setVolume(1f, 1f)
            ic_sound.setImageResource(R.drawable.ic_volume_off)
            mute_tv.text = context.resources.getString(R.string.livelike_mute_label)
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    /** extract thumbnail from the video url */
    private fun setFrameThumbnail(videoUrl: String) {
        thumbnailView.visibility = VISIBLE
        ic_play.visibility = VISIBLE
        progress_bar.visibility = GONE
        playbackErrorView.visibility = GONE
        ic_play.setImageResource(R.drawable.ic_play_button)
        playerView.visibility = INVISIBLE
        var requestOptions = RequestOptions()

        if (videoUrl.isNotEmpty()) {
            requestOptions = if (viewModel?.data?.latest()?.title.isNullOrEmpty()) {
                requestOptions.transforms(CenterCrop(), GranularRoundedCorners(16f, 16f, 16f, 16f))
            } else {
                requestOptions.transforms(CenterCrop(), GranularRoundedCorners(0f, 0f, 16f, 16f))
            }
            Glide.with(context.applicationContext)
                .asBitmap()
                .load(videoUrl)
                .apply(requestOptions)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .thumbnail(0.1f)
                .into(thumbnailView)
        }
    }

    private fun defaultStateTransitionManager(widgetStates: WidgetStates?) {
        when (widgetStates) {
            WidgetStates.READY -> {
                viewModel?.widgetState?.onNext(WidgetStates.INTERACTING)
            }
            WidgetStates.INTERACTING -> {
                viewModel?.data?.latest()?.let {
                    viewModel?.startDismissTimeout(it.timeout) {
                        viewModel?.widgetState?.onNext(WidgetStates.FINISHED)
                    }
                }
            }
            WidgetStates.FINISHED -> {
                removeAllViews()
                parent?.let { (it as ViewGroup).removeAllViews() }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun setPlayerViewCornersRound(isOnlyBottomCornersToBeRounded: Boolean) {
        playerView.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val corner = 20f
                if (isOnlyBottomCornersToBeRounded) {
                    outline.setRoundRect(0, -corner.toInt(), view.width, view.height, corner)
                } else {
                    outline.setRoundRect(
                        0,
                        0,
                        view.width,
                        view.height,
                        corner
                    ) // for making all corners rounded
                }
            }
        }

        playerView.clipToOutline = true
    }

    private fun openBrowser(context: Context, linkUrl: String) {
        viewModel?.onVideoAlertClickLink(linkUrl)
        val universalLinkIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (universalLinkIntent.resolveActivity(context.packageManager) != null) {
            ContextCompat.startActivity(context, universalLinkIntent, Bundle.EMPTY)
        }
    }
}
