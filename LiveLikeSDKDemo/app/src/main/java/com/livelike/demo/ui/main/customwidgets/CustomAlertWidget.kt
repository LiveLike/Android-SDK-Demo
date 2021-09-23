package com.livelike.demo.ui.main.customwidgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.livelike.demo.R
import com.livelike.engagementsdk.widget.widgetModel.AlertWidgetModel
import kotlinx.android.synthetic.main.custom_alert_widget.view.bodyImage
import kotlinx.android.synthetic.main.custom_alert_widget.view.bodyText
import kotlinx.coroutines.*


class CustomAlertWidget : ConstraintLayout {
    private val job = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)
    lateinit var alertModel: AlertWidgetModel

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        inflate(context, R.layout.custom_alert_widget, this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        alertModel.widgetData.let { likeWidget ->

            bodyText.text = likeWidget.title
            likeWidget.imageUrl?.let {
                bodyImage.visibility = View.VISIBLE
                Glide.with(context)
                    .load(it)
                    .into(bodyImage)
            }
            val timeMillis = likeWidget.timeout?.parseDuration() ?: 5000
            uiScope.async {
                delay(timeMillis)
                alertModel.finish()
            }
        }
    }


}