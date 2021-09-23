package com.livelike.demo

import android.content.DialogInterface
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.SimpleExoPlayerView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.material.tabs.TabLayout
import com.livelike.demo.ui.main.*


class DemoMainActivity : AppCompatActivity() {
    private val pageViewModel: PageViewModel by viewModels()
    private var player: SimpleExoPlayer? = null
    private var playerView: SimpleExoPlayerView? = null
    private var playbackPosition = 0L
    private var playWhenReady = true
    private var currentWindow: Int = 0

    private var rankTextView: TextView? = null
    private var ptsTextView: TextView? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rankTextView = findViewById(R.id.rank_value)
        ptsTextView = findViewById(R.id.pts)
        pageViewModel.currentUserLeaderBoard.observe(this, Observer {
            rankTextView?.setText("#"+it.rank)
            ptsTextView?.setText(it.score.toString()+" pts")
        })

        playerView = findViewById(R.id.video_view)
        showAlert()

        Glide.with(baseContext).load("https://websdk.livelikecdn.com/demo/assets/images/blackrobot.png").into(
            findViewById<View>(R.id.avtar) as ImageView
        )
        findViewById<View>(R.id.trophy).setOnClickListener {
            val leaderBoardEntryFrag = LeaderBoardEntryListDialogFragment.newInstance(75)
            leaderBoardEntryFrag.show(supportFragmentManager, LeaderBoardEntryListDialogFragment.TAG)
        }
    }

    private fun showAlert() {
        val activityContext = baseContext

        val viewGroup = findViewById<ViewGroup>(android.R.id.content)
        val dialogView: View =
            LayoutInflater.from(activityContext).inflate(R.layout.dialog_view, viewGroup, false)
        val clientIdEditText =dialogView.findViewById<EditText>(R.id.client_id)
        val programIdEditText =dialogView.findViewById<EditText>(R.id.program_id)
        val publicChatRoomEditText =dialogView.findViewById<EditText>(R.id.public_chat)
        val influencerEditText =dialogView.findViewById<EditText>(R.id.influencer_chat)
        val errorLabel =dialogView.findViewById<TextView>(R.id.error_label)
        val sharedPreferences = activityContext.getSharedPreferences(ID_SHARED_PREFS, MODE_PRIVATE)

        val alertDialog: AlertDialog? = this?.let {
            val builder = AlertDialog.Builder(it)

            builder.apply {

                setTitle("Enter Your Client Id, Program Id, Chat Room Ids")
                setView(dialogView)
                clientIdEditText.setText(sharedPreferences.getString(CLIENT_ID_KEY,PageViewModel.LIVELIKE_CLIENT_ID))
                programIdEditText.setText(sharedPreferences.getString(PROGRAM_ID_KEY,PageViewModel.CONTENT_PROGRAM_ID))
                publicChatRoomEditText.setText(sharedPreferences.getString(PUBLIC_CHAT_ID_KEY,"1b8792b2-eadc-4355-a323-72a028745e0b"))
                influencerEditText.setText(sharedPreferences.getString(INFLUENCER_CHAT_ID_KEY,"39a245dd-2a83-42e8-83fb-442d7adad0f6"))
            }

            // Create the AlertDialog
            builder.create()
        }

        alertDialog?.apply {
            dialogView.findViewById<Button>(R.id.saveButton).setOnClickListener() {
                val clientId = clientIdEditText.text
                errorLabel.setText("")
                if(clientId.isNullOrEmpty()) {
                    errorLabel.setText("Please enter Client Id")
                    return@setOnClickListener
                }
                if(programIdEditText.text.isNullOrEmpty()) {
                    errorLabel.setText("Please enter Program Id")
                    return@setOnClickListener
                }
                if(publicChatRoomEditText.text.isNullOrEmpty()) {
                    errorLabel.setText("Please enter Public Chat Room id")
                    return@setOnClickListener
                }
                if(influencerEditText.text.isNullOrEmpty()) {
                    errorLabel.setText("Please enter Influencer Chat room id")
                    return@setOnClickListener
                }

                val editor = sharedPreferences.edit()
                editor.putString(CLIENT_ID_KEY,clientId.toString())
                editor.putString(PROGRAM_ID_KEY,programIdEditText.text.toString())
                editor.putString(PUBLIC_CHAT_ID_KEY,publicChatRoomEditText.text.toString())
                editor.putString(INFLUENCER_CHAT_ID_KEY,influencerEditText.text.toString())
                editor.commit()
                this.dismiss()
            }
        }
        alertDialog?.show()
        alertDialog?.setOnDismissListener() {
            pageViewModel.initEnagementSDK(applicationContext)

            val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
            val viewPager: ViewPager = findViewById(R.id.view_pager)
            viewPager.adapter = sectionsPagerAdapter
            val tabs: TabLayout = findViewById(R.id.tabs)
            tabs.setupWithViewPager(viewPager)

            (findViewById(R.id.profile_view_container) as View).setOnClickListener({
                //Show Profile
            })
            pageViewModel.widgetJsonData.observe(this, {
                if (tabs.selectedTabPosition != 2) {
                    val widgetSheet: BottomWidgetPopUp = BottomWidgetPopUp.newInstance(it)
                    widgetSheet.show(supportFragmentManager, BottomWidgetPopUp.TAG)
                }
            })
        }
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onResume() {
        super.onResume()
        initializePlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    override fun onPause() {
        super.onPause()
        releasePlayer()
    }

    private fun initializePlayer() {
        if (player == null) {
            player = ExoPlayerFactory.newSimpleInstance(
                DefaultRenderersFactory(applicationContext),
                DefaultTrackSelector(),
                DefaultLoadControl()
            )
            playerView?.player = player
            player?.playWhenReady = true
            player?.seekTo(currentWindow, playbackPosition)
        }
        val mediaSource =
            buildMediaSource(Uri.parse("https://websdk.livelikecdn.com/demo/manchestervid.mp4"))
        player?.prepare(mediaSource, true, false)
    }

    private fun buildMediaSource(uri: Uri): MediaSource {

        val userAgent = "exoplayer-codelab"

        if (uri.lastPathSegment!!.contains("mp3") || uri.lastPathSegment!!
                .contains("mp4")
        ) {
            return ExtractorMediaSource.Factory(DefaultHttpDataSourceFactory(userAgent))
                .createMediaSource(uri)
        } else if (uri.lastPathSegment!!.contains("m3u8")) {
            return HlsMediaSource.Factory(DefaultHttpDataSourceFactory(userAgent))
                .createMediaSource(uri)
        } else {
            //  val BANDWIDTH_METER
            return HlsMediaSource.Factory(DefaultHttpDataSourceFactory(userAgent))
                .createMediaSource(uri)
        }
    }

    private fun releasePlayer() {
        if (player != null) {
            playbackPosition = player!!.currentPosition
            currentWindow = player!!.currentWindowIndex
            playWhenReady = player!!.playWhenReady
            player!!.release()
            player = null
        }
    }

    companion object {
        const val ID_SHARED_PREFS = "stored_ids"
        const val CLIENT_ID_KEY = "saved_client_id"
        const val PROGRAM_ID_KEY = "saved_program_id"
        const val PUBLIC_CHAT_ID_KEY = "saved_public_chat_id"
        const val INFLUENCER_CHAT_ID_KEY = "saved_influencer_id"

    }
}