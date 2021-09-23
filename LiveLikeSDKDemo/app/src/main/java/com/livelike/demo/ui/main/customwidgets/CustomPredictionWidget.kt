package com.livelike.demo.ui.main.customwidgets

import android.content.Context
import android.os.Handler
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import android.util.AttributeSet
import android.view.View
import com.livelike.demo.R
import com.livelike.demo.ui.main.PollListAdapter
import com.livelike.engagementsdk.widget.widgetModel.FollowUpWidgetViewModel
import com.livelike.engagementsdk.widget.widgetModel.PredictionWidgetViewModel
import kotlinx.android.synthetic.main.custom_poll_widget.view.*

class CustomPredictionWidget :
    ConstraintLayout {
    var predictionWidgetViewModel: PredictionWidgetViewModel? = null
    var followUpWidgetViewModel: FollowUpWidgetViewModel? = null
    var isImage = false
    var isFollowUp = false

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
        inflate(context, R.layout.custom_prediction_widget, this@CustomPredictionWidget)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        var widgetData = predictionWidgetViewModel?.widgetData
        var voteResults = predictionWidgetViewModel?.voteResults
        if (isFollowUp) {
            widgetData = followUpWidgetViewModel?.widgetData
            voteResults = followUpWidgetViewModel?.voteResults
        }

        widgetData?.let { liveLikeWidget ->
            liveLikeWidget.options?.let {
                if (it.size > 2) {
                    textRecyclerView.layoutManager =
                        GridLayoutManager(
                            context,
                            2
                        )
                }
                val adapter =
                    PollListAdapter(context, isImage, ArrayList(it.map { item -> item!! }))
                textRecyclerView.adapter = adapter
                adapter.pollListener = object : PollListAdapter.PollListener {
                    override fun onSelectOption(id: String) {
                        predictionWidgetViewModel?.lockInVote(id)
                    }
                }
                //button2.visibility = View.GONE
                voteResults?.subscribe(this) { result ->
                    result?.choices?.let { options ->
                        options.forEach { op ->
                            adapter.optionIdCount[op.id] = op.vote_count ?: 0
                        }
                        adapter.notifyDataSetChanged()
                    }
                }
                if(isFollowUp){
                    it.forEach { op ->
                        adapter.optionIdCount[op?.id!!] = op.voteCount ?: 0
                    }
                    adapter.isFollowUp = true
                    adapter.selectedIndex = it.indexOfFirst { option-> option?.id == followUpWidgetViewModel?.getPredictionVoteId() }
                    adapter.notifyDataSetChanged()
                    followUpWidgetViewModel?.claimRewards()
                }
            }
           /* imageView2.setOnClickListener {
                finish()
            }
*/
            val handler = Handler()
            handler.postDelayed({
                finish()
            }, (liveLikeWidget.timeout ?: "").parseDuration())

        }
    }

    private fun finish() {
        predictionWidgetViewModel?.finish()
        followUpWidgetViewModel?.finish()
    }


    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        predictionWidgetViewModel?.voteResults?.unsubscribe(this)
    }
}