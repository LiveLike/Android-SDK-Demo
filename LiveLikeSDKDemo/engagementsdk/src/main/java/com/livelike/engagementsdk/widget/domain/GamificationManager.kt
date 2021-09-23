package com.livelike.engagementsdk.widget.domain

import com.google.gson.JsonObject
import com.livelike.engagementsdk.core.services.messaging.ClientMessage
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.widget.WidgetManager
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.data.models.ProgramGamificationProfile

/**
 * All domain logic and use-cases related to Gamification.
 * we can split it later based on usecases or not singleton. My heartly welcome for it.
 */
internal object GamificationManager {

    /** Check for latest badges and push the coolect badge widget into widget messaging client
     * @param program reward object that is typically fetched after widget interaction .
     * @param widgetManager last layer in widget pipeline where new badge collection widget may be published.
     */
    fun checkForNewBadgeEarned(
        programGamificationProfile: ProgramGamificationProfile,
        widgetManager: WidgetManager
    ) {

        if (programGamificationProfile.newBadges != null && programGamificationProfile.newBadges.isNotEmpty()) {
            val latestBadge = programGamificationProfile.newBadges.maxOrNull()
            val message = ClientMessage(
//                TODO create generic to create this json message, really tech debt is increasing at fast rate now.
                JsonObject().apply {
                    addProperty("event", WidgetType.COLLECT_BADGE.event)
                    add("payload", gson.toJsonTree(latestBadge).asJsonObject)
                    addProperty("priority", 2)
                }
            )
            widgetManager.onClientMessageEvent(widgetManager, message)
        }
    }
}
