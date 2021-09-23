package com.livelike.engagementsdk.widget

import android.content.res.TypedArray
import com.livelike.engagementsdk.R

class WidgetViewThemeAttributes {

    var widgetWinAnimation: String = "winAnimation"
    var widgetLoseAnimation: String = "loseAnimation"
    var widgetDrawAnimation: String = "drawAnimation"
    var stayTunedAnimation: String = "staytuned"

    fun init(typedArray: TypedArray?) {
        typedArray?.apply {
            widgetWinAnimation =
                getString(R.styleable.WidgetView_winAnimation) ?: "winAnimation"
            widgetLoseAnimation =
                getString(R.styleable.WidgetView_loseAnimation) ?: "loseAnimation"
            widgetDrawAnimation =
                getString(R.styleable.WidgetView_drawAnimation) ?: "drawAnimation"
            stayTunedAnimation =
                getString(R.styleable.WidgetView_stayTunedAnimation) ?: "staytuned"
        }
    }
}
