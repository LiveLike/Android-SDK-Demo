package com.livelike.demo.ui.main


import android.content.Context
import android.graphics.Color
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.livelike.demo.R
import com.livelike.engagementsdk.OptionsItem
import com.livelike.engagementsdk.widget.widgetModel.PollWidgetModel
import kotlinx.android.synthetic.main.custom_poll_widget.view.*
import kotlinx.android.synthetic.main.custom_poll_widget.view.textRecyclerView
import kotlinx.android.synthetic.main.quiz_image_list_item.view.imageButton2
import kotlinx.android.synthetic.main.quiz_image_list_item.view.textView8
import kotlinx.android.synthetic.main.quiz_list_item.view.button4
import kotlinx.android.synthetic.main.quiz_list_item.view.textView7

class CustomPollWidget : ConstraintLayout {
    var pollWidgetModel: PollWidgetModel? = null
    var isImage = false

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
        inflate(context, R.layout.custom_poll_widget, this@CustomPollWidget)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        pollWidgetModel?.widgetData?.let { liveLikeWidget ->
            titleView.text = liveLikeWidget.question.toString()
                liveLikeWidget.options?.let {
                if (it.size > 2) {
                    textRecyclerView.layoutManager = LinearLayoutManager(context)
                }

                val adapter =
                    PollListAdapter(context, isImage, ArrayList(it.map { item -> item!! }))
                textRecyclerView.adapter = adapter
                adapter.pollListener = object : PollListAdapter.PollListener {
                    override fun onSelectOption(id: String) {
                        pollWidgetModel?.submitVote(id)
                    }
                }
                pollWidgetModel?.voteResults?.subscribe(this) { result ->
                    result?.choices?.let { options ->
                        options.forEach { op ->
                            adapter.optionIdCount[op.id] = op.vote_count ?: 0
                        }
                        adapter.notifyDataSetChanged()
                    }
                }
            }
            /*imageView2.setOnClickListener {
                pollWidgetModel?.finish()
            }*/

        }

    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pollWidgetModel?.voteResults?.unsubscribe(this)
    }
}

class PollListAdapter(
    private val context: Context,
    private val isImage: Boolean,
    val list: ArrayList<OptionsItem>
) :
    RecyclerView.Adapter<PollListAdapter.PollListItemViewHolder>() {
    var selectedIndex = -1
    val optionIdCount: HashMap<String, Int> = hashMapOf()

    var isFollowUp = false

    fun getSelectedOption(): OptionsItem? = when (selectedIndex > -1) {
        true -> list[selectedIndex]
        else -> null
    }

    var pollListener: PollListener? = null

    interface PollListener {
        fun onSelectOption(id: String)
    }

    class PollListItemViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): PollListItemViewHolder {
        return PollListItemViewHolder(
            LayoutInflater.from(p0.context!!).inflate(
                when (isImage) {
                    true -> R.layout.quiz_image_list_item
                    else -> R.layout.quiz_list_item
                }, p0, false
            )
        )
    }

    override fun onBindViewHolder(holder: PollListItemViewHolder, index: Int) {
        val item = list[index]
        if (isImage) {
            Glide.with(context)
                .load(item.imageUrl)
                .into(holder.itemView.imageButton2)
            if (selectedIndex == index) {
                holder.itemView.imageButton2.setBackgroundColor(Color.BLUE)
            } else {
                holder.itemView.imageButton2.setBackgroundColor(Color.GRAY)
            }
            holder.itemView.textView8.text = "${optionIdCount[item.id!!] ?: 0}"

            holder.itemView.imageButton2.setOnClickListener {
                selectedIndex = holder.adapterPosition
                pollListener?.onSelectOption(item.id!!)
                notifyDataSetChanged()
            }
        } else {

            holder.itemView.textView7.text = "${optionIdCount[item.id!!] ?: 0}"
            holder.itemView.button4.text = "${item.description}"
            if (selectedIndex == index) {
               // holder.itemView.button4.setBackgroundColor(Color.BLUE)
            } else {
               // holder.itemView.button4.setBackgroundColor(Color.GRAY)
            }

            holder.itemView.button4.setOnClickListener {
                if(!isFollowUp) {
                    selectedIndex = holder.adapterPosition
                    pollListener?.onSelectOption(item.id!!)
                    notifyDataSetChanged()
                }
            }
        }

    }

    override fun getItemCount(): Int = list.size
}
