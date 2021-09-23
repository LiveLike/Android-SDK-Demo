package com.livelike.demo.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import com.livelike.demo.R
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.widget.view.WidgetView

class BottomWidgetPopUp : BottomSheetDialogFragment() {
    lateinit var widgetView : WidgetView
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.bottom_sheet, container, false)
        widgetView = root.findViewById(R.id.widget_view)
        val widgetJson = arguments?.getString(WIDGET_MESSAGE) ?: ""
        val pageViewModel = ViewModelProvider(requireActivity()).get(PageViewModel::class.java)
       if (!widgetJson.isNullOrEmpty()) {

            pageViewModel.engagementSDK.fetchWidgetDetails("a22ff6ee-8653-454b-be9e-a9791c3bb0cb","text-quiz", object: LiveLikeCallback<LiveLikeWidget>() {
                override fun onResponse(result: LiveLikeWidget?, error: String?) {
                    pageViewModel.engagementSDK.let {
                        if (result != null) {
                            //widgetView.displayWidget(it, result )

                        }
                    }
                }

            })
            pageViewModel.engagementSDK.let { widgetView.displayWidget(it, Gson().fromJson(widgetJson,LiveLikeWidget::class.java) ) }
       }
        return root
    }

    companion object {
        const val TAG = "ActionBottomDialog"
        private const val WIDGET_MESSAGE = "WIDGET_MESSAGE"
        fun newInstance(widgetDataJson: String): BottomWidgetPopUp {
            return BottomWidgetPopUp().apply {
                arguments = Bundle().apply {
                    putString(WIDGET_MESSAGE, widgetDataJson)
                }
            }
        }
    }
}