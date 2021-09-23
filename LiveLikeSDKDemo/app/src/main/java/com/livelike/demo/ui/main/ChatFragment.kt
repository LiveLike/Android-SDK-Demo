package com.livelike.demo.ui.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.livelike.demo.R
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.chat.ChatView
import com.livelike.engagementsdk.publicapis.ErrorDelegate
import com.livelike.engagementsdk.publicapis.LiveLikeCallback

/**
 * A placeholder fragment containing a simple view.
 */
class ChatFragment : BaseFragment() {

    private lateinit var pageViewModel: PageViewModel
    private var programId = ""
    private var isChatInputVisible = false
    var chat_view : ChatView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProvider(requireActivity()).get(PageViewModel::class.java)
        setProgramId(arguments?.getString(ARG_SECTION_NUMBER) ?: "")
        isChatInputVisible = arguments?.getBoolean(ARG_CHAT_INPUT_VISIBILITY) ?: false
    }

    private fun setProgramId(programId: String) {
        this.programId = programId

    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.chat_fragment_layout, container, false)
        val chatView: ChatView = root.findViewById(R.id.chat_view)
        initChatSession(chatView)
        return root
    }

    private fun initChatSession(chat_view: ChatView) {

        pageViewModel.chatFrag= this
        val chatSession = pageViewModel.engagementSDK.createChatSession(object : EngagementSDK.TimecodeGetter {
            override fun getTimecode(): EpochTime {
                return EpochTime(0)
            }

        }, errorDelegate = object : ErrorDelegate() {
            override fun onError(error: String) {
                Log.e("TEST", error)
            }
        })

        if (chatSession != null) {
            chatSession.connectToChatRoom(this.programId, callback = object : LiveLikeCallback<Unit>() {
                override fun onResponse(result: Unit?, error: String?) {
                    if (error != null) {
                        Log.e("TEST", error)
                    }
                }
            })
            chat_view.allowMediaFromKeyboard = true
            chat_view.isChatInputVisible = isChatInputVisible
            chat_view.setSession(chatSession)
            //chat_view.clearSession()
            this.chat_view = chat_view
            //chatSession.close()
        }
    }


    companion object {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val ARG_SECTION_NUMBER = "section_number"
        private const val ARG_CHAT_INPUT_VISIBILITY = "chat_input_visibility"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        @JvmStatic
        fun newInstance(sectionNumber: String, isChatInputVisible: Boolean): ChatFragment {
            return ChatFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SECTION_NUMBER, sectionNumber)
                    putBoolean(ARG_CHAT_INPUT_VISIBILITY,isChatInputVisible)
                }
            }
        }
    }
}