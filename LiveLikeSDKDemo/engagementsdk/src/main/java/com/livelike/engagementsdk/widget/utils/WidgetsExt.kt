package com.livelike.engagementsdk.widget.utils

import com.livelike.engagementsdk.widget.WidgetType

fun WidgetType.toAnalyticsString(): String {
    return when (this) {
        WidgetType.TEXT_POLL -> "Text Poll"
        WidgetType.IMAGE_POLL -> "Image Poll"
        WidgetType.IMAGE_PREDICTION -> "Image Prediction"
        WidgetType.IMAGE_PREDICTION_FOLLOW_UP -> "Image Prediction Follow-up"
        WidgetType.TEXT_PREDICTION -> "Text Prediction"
        WidgetType.TEXT_PREDICTION_FOLLOW_UP -> "Text Prediction Follow-up"
        WidgetType.IMAGE_QUIZ -> "Image Quiz"
        WidgetType.TEXT_QUIZ -> "Text Quiz"
        WidgetType.ALERT -> "Alert"
        WidgetType.POINTS_TUTORIAL -> "Points Tutorial"
        WidgetType.COLLECT_BADGE -> "Collect Badge"
        WidgetType.CHEER_METER -> "Cheer Meter"
        WidgetType.IMAGE_SLIDER -> "Image Slider"
        WidgetType.SOCIAL_EMBED -> "Social Embed"
        WidgetType.VIDEO_ALERT -> "Video Alert"
        WidgetType.TEXT_ASK -> "Text Ask"
    }
}
