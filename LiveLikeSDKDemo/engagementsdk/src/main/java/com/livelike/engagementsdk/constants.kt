package com.livelike.engagementsdk

import java.text.SimpleDateFormat
import java.util.Locale

internal const val BUGSNAG_ENGAGEMENT_SDK_KEY = "abb12b7b7d7868c07733e3e3808656c8"

internal const val CHAT_HISTORY_LIMIT = 25

internal const val CHAT_PROVIDER = "pubnub"
internal const val REACTION_CREATED = "rc"

// RestApi path and param keys template

internal const val TEMPLATE_PROGRAM_ID = "{program_id}"
internal const val TEMPLATE_CHAT_ROOM_ID = "{chat_room_id}"
internal const val TEMPLATE_LEADER_BOARD_ID = "{leaderboard_id}"
internal const val TEMPLATE_PROFILE_ID = "{profile_id}"

// Date time formatters

internal val DEFAULT_CHAT_MESSAGE_DATE_TIIME_FROMATTER = SimpleDateFormat(
    "MMM d, h:mm a",
    Locale.getDefault()
)
