package com.livelike.engagementsdk.core.analytics

enum class AnalyticsSuperProperties(val key: String, val isPeopleProperty: Boolean) {

    POINTS_THIS_PROGRAM("Points This Program", false),
    TIME_LAST_BADGE_AWARD("Time of Last Badge Award", true),
    BADGE_LEVEL_THIS_PROGRAM("Badge Level This Program", false)
}
