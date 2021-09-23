package com.livelike.engagementsdk.widget.timeline

import com.livelike.engagementsdk.LiveLikeWidget

/**
 * This class is responsible for returning the time value for widget timer,
 * all the default widget timer respects this value if it is defined
 */
abstract class WidgetTimerController {

    /**
     * return the time value in ISO-8601 duration format, timer will run for the duration returned here
     */
    abstract fun timeValue(widget: LiveLikeWidget): String
}

/**
 * This is implementation of WidgetTimerController which simply returns the cms defined duration for widget timer
 */
class CMSSpecifiedDurationTimer : WidgetTimerController() {

    override fun timeValue(widget: LiveLikeWidget): String {
        return widget.timeout ?: "P0DT00H00M30S"
    }
}
