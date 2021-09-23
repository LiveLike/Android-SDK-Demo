package com.livelike.engagementsdk.widget.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.livelike.engagementsdk.MockAnalyticsService
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.core.data.models.RewardsType
import com.livelike.engagementsdk.core.data.respository.ProgramRepository
import com.livelike.engagementsdk.core.data.respository.UserRepository
import com.livelike.engagementsdk.widget.view.components.PointsTutorialView
import com.livelike.engagementsdk.widget.viewModel.PointTutorialWidgetViewModel
import kotlinx.android.synthetic.main.widget_test_view.view.buttonRefresh
import kotlinx.android.synthetic.main.widget_test_view.view.testFirst
import kotlinx.android.synthetic.main.widget_test_view.view.testFourth
import kotlinx.android.synthetic.main.widget_test_view.view.testSecond
import kotlinx.android.synthetic.main.widget_test_view.view.testThird

class WidgetTestView(context: Context, attr: AttributeSet) : FrameLayout(context, attr) {

//    private val mockConfig = EngagementSDK.SdkConfiguration("", "", "", "", "sub-c-016db434-d156-11e8-b5de-7a9ddb77e130", "", "", "", "", "", "", "", mapOf(), "", "", "")

    private val textLabels = listOf(
        "NEW RECORD",
        "SPONSOR",
        "UPCOMING",
        "",
        "NBA"
    )

    private val textTitle = listOf(
        "IS THIS ELI MANNING'S LAST GAME WITH THE GIANTS?",
        "WHO IS YOUR MVP THIS YEAR?",
        "WHICH WAS YOUR TEAM LAST YEAR?",
        "WHAT KIND OF FAN ARE YOU?",
        "WHO WILL BE THE MAN OF THE MATCH?",
        "WHO HOLDS THE RECORD FOR MOST TOUCHDOWNS IN NFL HISTORY?"
    )

    private val textOptions = listOf(
        "Who? Is he still playing?",
        "He is the man.",
        "Tom Brady",
        "For sure! He's done.",
        "He still has a lot left in the tank!",
        "I'm not interested..."
    )

    private val bodyOptions = listOf(
        "Super Bowl halftime show left shark by Katy Perry",
        "",
        "Dirk Koetter stays with Ryan Fitzpatrick to save season, but is it realistic?",
        "Manning reaches 500 TD passes in Broncos' 38-24 win"
    )

    private val linkOptions = listOf(
        "",
        "https://google.fr"
    )

    private val linkLabelOptions = listOf(
        "Click Here",
        "Enjoy more Here",
        "Do you want more?"
    )

    private val imageUrlOption = listOf(
        "",
        "https://picsum.photos/150/150?"
    )

    var imageUrl: () -> String = { "" }

    private val dataAlert =
        {
            """
            {"timeout": "P0DT00H00M10S",
              "kind": "alert",
              "program_date_time": null,
              "title": "${textLabels.first()}",
              "text": "${bodyOptions.first()}",
              "image_url": "https://picsum.photos/150/100?${java.util.UUID.randomUUID()}",
              "link_url": "${linkOptions.first()}",
              "link_label": "${linkLabelOptions.first()}"}
        """
        }

    private val pollTextData =
        { """{"id":"9d1b221c-50e9-4b4d-8d0e-2ddb250364f3","question":"${textTitle.first()}","timeout":"P0DT01H00M10S","options":[{"id":"9e9b519c-bec3-40a5-95b2-2ca8c89a41ba","description":"${textOptions.first()}","image_url":"${imageUrl()}","vote_count":0,"vote_url":"https://cf-blast.livelikecdn.com/api/v1/text-poll-options/9e9b519c-bec3-40a5-95b2-2ca8c89a41ba/votes/"},{"id":"65441e5e-fac8-4260-8cee-792120dd976b","description":"${textOptions.first()}","image_url":"${imageUrl()}","vote_count":0,"vote_url":"https://cf-blast.livelikecdn.com/api/v1/text-poll-options/65441e5e-fac8-4260-8cee-792120dd976b/votes/"}],"subscribe_channel":"text_poll_9d1b221c_50e9_4b4d_8d0e_2ddb250364f3","program_date_time":null}""" }

    private val quizTextData =
        { """{"timeout":"P0DT00H00M03S","kind":"text-quiz","program_date_time":null,"subscribe_channel":"text_quiz_aca0ef1f_bfd5_48cd_90e2_6bfba3d32057","question":"${textTitle.first()}","choices":[{"id":"${java.util.UUID.randomUUID()}","image_url":"${imageUrl()}", "description":"${textOptions.first()}","is_correct":true,"answer_url":"https://cf-blast.livelikecdn.com/api/v1/text-quiz-choices/866bab19-60d8-40d9-89b9-7be3b065e7be/answers/","answer_count":0},{"id":"${java.util.UUID.randomUUID()}","image_url":"${imageUrl()}", "description":"${textOptions.first()}","is_correct":true,"answer_url":"https://cf-blast.livelikecdn.com/api/v1/text-quiz-choices/5d6add12-4111-4137-8f50-95be775be012/answers/","answer_count":0}]}""" }

    private val predictionTextData =
        { """{"timeout":"P0DT00H00M03S","kind":"text-prediction","program_date_time":null,"subscribe_channel":"text_prediction_710a9bef_9932_493b_a414_e9a37abf49d6","question":"${textTitle.first()}","confirmation_message":"${textOptions.first()}","options":[{"image_url":"${imageUrl()}", "url":"","description":"${textOptions.first()}","is_correct":false,"vote_count":0,"vote_url":""},{"image_url":"${imageUrl()}", "url":"","description":"${textOptions.first()}","is_correct":false,"vote_count":0,"vote_url":""}]}""" }

    private var userRepository =
        UserRepository("")
    private var programRepository =
        ProgramRepository(
            "",
            userRepository
        )

    init {
        ConstraintLayout.inflate(context, R.layout.widget_test_view, this)

        buttonRefresh.setOnClickListener {
            cleanupViews()
            addWidgetViews()
        }
    }

    private fun cleanupViews() {
        testFirst.removeAllViews()
        testSecond.removeAllViews()
        testThird.removeAllViews()
        testFourth.removeAllViews()
    }

    private fun addWidgetViews() {
        val randomImage = imageUrlOption.first()
        imageUrl =
            { if (randomImage.isEmpty()) randomImage else randomImage + java.util.UUID.randomUUID() }
        val viewTutorial = PointsTutorialView(context).apply {
        }
        viewTutorial.widgetViewModel = PointTutorialWidgetViewModel(
            {},
            MockAnalyticsService(),
            RewardsType.BADGES,
            programRepository.programGamificationProfileStream.latest()
        )
        testSecond.addView(viewTutorial)
//        val viewAlert = AlertWidgetView(context).apply {
//            val info = WidgetInfos(
//                "alert-created",
//                gson.fromJson(dataAlert(), JsonObject::class.java),
//                "120571e0-d665-4e9b-b497-908cf8422a64"
//            )
//            widgetViewModel = AlertWidgetViewModel(info,
//                MockAnalyticsService()
//            ) {}
//        }
//        val viewPoll = PollView(context).apply {
//            val info = WidgetInfos(
//                "text-poll-created",
//                gson.fromJson(pollTextData(), JsonObject::class.java),
//                "120571e0-d665-4e9b-b497-908cf8422a64"
//            )
//            widgetViewModel = PollViewModel(
//                info,
//                MockAnalyticsService(),
//                mockConfig,
//                {},
//                userRepository,
//                programRepository,
//                widgetMessagingClient
//            )
//        }
//        val viewQuiz = QuizView(context).apply {
//            val info = WidgetInfos(
//                "text-quiz-created",
//                gson.fromJson(quizTextData(), JsonObject::class.java),
//                "120571e0-d665-4e9b-b497-908cf8422a64"
//            )
//            widgetViewModel = QuizViewModel(
//                info,
//                MockAnalyticsService(),
//                mockConfig,
//                context,
//                {},
//                userRepository,
//                programRepository,
//                widgetMessagingClient
//            )
//        }
//        val viewPrediction = PredictionView(context).apply {
//            val info = WidgetInfos(
//                "text-prediction-created",
//                gson.fromJson(predictionTextData(), JsonObject::class.java),
//                "120571e0-d665-4e9b-b497-908cf8422a64"
//            )
//            widgetViewModel = PredictionViewModel(
//                info, context,
//                MockAnalyticsService(), {}, userRepository, programRepository, widgetMessagingClient
//            )
//        }
//
//        testFirst.addView(viewPrediction)
//        testSecond.addView(viewPoll)
//        testThird.addView(viewQuiz)
//        testFourth.addView(viewAlert)
    }
}
