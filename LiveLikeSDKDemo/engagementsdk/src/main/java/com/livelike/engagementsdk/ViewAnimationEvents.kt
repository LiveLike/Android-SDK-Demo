package com.livelike.engagementsdk

/**
 * All events that will be published on animation stream shared across different view components to sync animation updates.
 * For instance, badge is collected on widget component then badge animation will took place inside chatView.
 */
internal enum class ViewAnimationEvents {

    BADGE_COLLECTED
}
