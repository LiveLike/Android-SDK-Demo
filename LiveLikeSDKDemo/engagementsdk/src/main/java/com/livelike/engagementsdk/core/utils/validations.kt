package com.livelike.engagementsdk.core.utils

import java.util.regex.Pattern

const val UUID_REGEX =
    "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"

fun validateUuid(uuid: String): Boolean {
    return Pattern.compile(UUID_REGEX).matcher(uuid).matches()
}
