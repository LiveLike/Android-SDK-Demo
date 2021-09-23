package com.livelike.demo.ui.main

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.gson.JsonParser
import com.livelike.demo.DemoMainActivity
import com.livelike.demo.R
import com.livelike.engagementsdk.LiveLikeEngagementTheme
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.widget.LiveLikeWidgetViewFactory
import com.livelike.engagementsdk.widget.timeline.WidgetTimeLineViewModel
import com.livelike.engagementsdk.widget.timeline.WidgetsTimeLineView
import com.livelike.engagementsdk.widget.widgetModel.*
import java.io.IOException
import java.io.InputStream

/**
 * A placeholder fragment containing a simple view.
 */
class WidgetTimeLineFragment : BaseFragment() {

    private lateinit var pageViewModel: PageViewModel
    private var programId = ""
    private lateinit var containerGrp: ViewGroup
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProvider(requireActivity()).get(PageViewModel::class.java)
        val sharedPreferences = context?.getSharedPreferences(DemoMainActivity.ID_SHARED_PREFS, Context.MODE_PRIVATE)
        pageViewModel.createContentSession(sharedPreferences?.getString(DemoMainActivity.PROGRAM_ID_KEY,""))
    }

    private fun setProgramId(programId: String) {
        this.programId = programId
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        attachViewAndSession()

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.timeline_fragment_layout, container, false)
        containerGrp = root.findViewById(R.id.container_view)
        return root
    }

    private fun attachViewAndSession() {

        var timeLineViewModel: WidgetTimeLineViewModel
        pageViewModel.contentSession.let {
            timeLineViewModel = WidgetTimeLineViewModel(it)

            val timeLineView = WidgetsTimeLineView(
                requireContext(),
                timeLineViewModel,
                pageViewModel.engagementSDK
            )

            timeLineView.setSeparator(ContextCompat.getDrawable(requireContext(), R.drawable.white_separator))
            timeLineView.widgetViewFactory = object : LiveLikeWidgetViewFactory {
                override fun createCheerMeterView(viewModel: CheerMeterWidgetmodel): View? {
                    return null/*CustomCheerMeter(requireContext()).apply {
                        this.cheerMeterWidgetModel = cheerMeterWidgetModel
                    }*/
                }

                override fun createAlertWidgetView(alertWidgetModel: AlertWidgetModel): View? {
                    return null/*CustomAlertWidget(requireContext()).apply {
                        this.alertModel = alertWidgetModel
                    }*/
                }

                override fun createQuizWidgetView(
                    quizWidgetModel: QuizWidgetModel,
                    isImage: Boolean
                ): View? {
                    return null/*CustomQuizWidget(requireContext()).apply {
                        this.quizWidgetModel = quizWidgetModel
                        this.isImage = isImage
                    }*/
                }

                override fun createTextAskWidgetView(imageSliderWidgetModel: TextAskWidgetModel): View? {
                    return null
                }

                override fun createVideoAlertWidgetView(videoAlertWidgetModel: VideoAlertWidgetModel): View? {
                    return null
                }


                override fun createPredictionWidgetView(
                    predictionViewModel: PredictionWidgetViewModel,
                    isImage: Boolean
                ): View? {
                    return null/*CustomPredictionWidget(requireContext()).apply {
                        this.predictionWidgetViewModel = predictionWidgetViewModel
                        this.isImage = isImage
                    }*/
                }

                override fun createPredictionFollowupWidgetView(
                    followUpWidgetViewModel: FollowUpWidgetViewModel,
                    isImage: Boolean
                ): View? {
                    return null
                }

                override fun createPollWidgetView(
                    pollWidgetModel: PollWidgetModel,
                    isImage: Boolean
                ): View? {
                    return null;/*CustomPollWidget(requireContext()).apply {
                        this.pollWidgetModel = pollWidgetModel
                        this.isImage = isImage
                    }*/
                }

                override fun createImageSliderWidgetView(imageSliderWidgetModel: ImageSliderWidgetModel): View? {
                    return null/*CustomImageSlider(requireContext()).apply {
                        this.imageSliderWidgetModel = imageSliderWidgetModel
                    }*/
                }
            }

            try {
                val inputStream: InputStream = requireActivity().assets.open("livelike_styles.json")
                val size: Int = inputStream.available()
                val buffer = ByteArray(size)
                inputStream.read(buffer)
                val theme = String(buffer)
                val result =
                    LiveLikeEngagementTheme.instanceFrom(JsonParser.parseString(theme).asJsonObject)
                if (result is Result.Success) {
                    timeLineView.applyTheme(result.data)
                } else {
                    Toast.makeText(
                        context,
                        "Unable to get the theme json",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
            containerGrp.addView(timeLineView)
        }

    }


    companion object {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val ARG_SECTION_NUMBER = "section_number"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        @JvmStatic
        fun newInstance(sectionNumber: String): WidgetTimeLineFragment {
            return WidgetTimeLineFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }
}