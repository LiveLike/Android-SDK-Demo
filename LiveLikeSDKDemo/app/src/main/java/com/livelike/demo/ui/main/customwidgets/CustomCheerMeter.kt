package com.livelike.demo.ui.main.customwidgets;

import android.content.Context
import android.os.CountDownTimer
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.livelike.demo.R
import com.livelike.engagementsdk.widget.widgetModel.CheerMeterWidgetmodel

import kotlinx.android.synthetic.main.custom_cheer_meter.view.btn_1
import kotlinx.android.synthetic.main.custom_cheer_meter.view.btn_2
import kotlinx.android.synthetic.main.custom_cheer_meter.view.img_close
import kotlinx.android.synthetic.main.custom_cheer_meter.view.progress_bar
import kotlinx.android.synthetic.main.custom_cheer_meter.view.speed_view_1
import kotlinx.android.synthetic.main.custom_cheer_meter.view.speed_view_2
import kotlinx.android.synthetic.main.custom_cheer_meter.view.txt_team1
import kotlinx.android.synthetic.main.custom_cheer_meter.view.txt_team2
import org.threeten.bp.Duration
import org.threeten.bp.format.DateTimeParseException


/**
 * TODO: document your custom view class.
 */
class CustomCheerMeter : ConstraintLayout {

    private lateinit var mCountDownTimer: CountDownTimer
    var cheerMeterWidgetModel: CheerMeterWidgetmodel? = null


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
        inflate(context, R.layout.custom_cheer_meter, this@CustomCheerMeter)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        cheerMeterWidgetModel?.voteResults?.subscribe(this.javaClass) {
            val op1 = it?.choices?.get(0)
            val op2 = it?.choices?.get(1)
            val vt1 = op1?.vote_count ?: 0
            val vt2 = op2?.vote_count ?: 0
            val total = vt1 + vt2
            if (total > 0) {
                val perVt1 = (vt1.toFloat() / total.toFloat()) * 100
                val perVt2 = (vt2.toFloat() / total.toFloat()) * 100
                speed_view_1.setSpeedAt(perVt1)
                speed_view_2.setSpeedAt(perVt2)
                txt_team1.text = "$vt1\n$perVt1"
                txt_team2.text = "$vt2\n$perVt2"
            }
        }

        cheerMeterWidgetModel?.widgetData?.let { livelikeWidget ->

            Glide.with(context)
                .load(livelikeWidget.options?.get(0)?.imageUrl)
                .into(btn_1)

            Glide.with(context)
                .load(livelikeWidget.options?.get(1)?.imageUrl)
                .into(btn_2)

            btn_1.setOnClickListener {
                cheerMeterWidgetModel?.submitVote(livelikeWidget.options?.get(0)?.id!!)
            }
            btn_2.setOnClickListener {
                cheerMeterWidgetModel?.submitVote(livelikeWidget.options?.get(1)?.id!!)
            }
            img_close.setOnClickListener {
                cheerMeterWidgetModel?.finish()
                mCountDownTimer.cancel()
            }

            val handler = Handler()
            handler.postDelayed({
                cheerMeterWidgetModel?.finish()
                mCountDownTimer.onFinish()
            }, (livelikeWidget.timeout ?: "").parseDuration())

            var i = 0
            val timer = (livelikeWidget.timeout ?: "").parseDuration()
            mCountDownTimer = object : CountDownTimer(timer, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    i++
                    progress_bar.progress = (i * 100 / (timer / 1000)).toInt()
                }

                override fun onFinish() {
                    //Do what you want
                    i++
                    progress_bar.progress = 100
                }
            }
            mCountDownTimer.start()

        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cheerMeterWidgetModel?.voteResults?.unsubscribe(this.javaClass)
    }


}

fun String.parseDuration(): Long {
    var timeout = 7000L
    try {
        timeout = Duration.parse(this).toMillis()
    } catch (e: DateTimeParseException) {
        Log.e("Error", "Duration $this can't be parsed.")
    }
    return timeout
}