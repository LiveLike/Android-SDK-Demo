package com.livelike.engagementsdk.widget.adapters

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.livelike.engagementsdk.FontFamilyProvider
import com.livelike.engagementsdk.widget.OptionsWidgetThemeComponent
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.model.Option
import com.livelike.engagementsdk.widget.view.components.WidgetItemView

internal class WidgetOptionsViewAdapter(
    internal var myDataset: List<Option>,
    private val widgetType: WidgetType,
    var correctOptionId: String? = null,
    var userSelectedOptionId: String = "",
    var component: OptionsWidgetThemeComponent? = null
) :
    RecyclerView.Adapter<WidgetOptionsViewAdapter.TextOptionViewHolder>() {

    var fontFamilyProvider: FontFamilyProvider? = null
    var selectedPosition = RecyclerView.NO_POSITION
    var onClick: ((Option) -> Unit)? = null

    var selectionLocked = false
    var showPercentage = false
        set(value) {
            if (field != value && value) {
                notifyDataSetChanged()
            }
            field = value
        }

    inner class TextOptionViewHolder(
        val textItemView: WidgetItemView,
        val onClick: ((selectedOption: Option) -> Unit)?
    ) :
        RecyclerView.ViewHolder(textItemView),
        View.OnClickListener {
        init {
            textItemView.clickListener = this
        }

        override fun onClick(v: View?) {
            if (adapterPosition == RecyclerView.NO_POSITION || selectionLocked || selectedPosition == adapterPosition) return

            selectedPosition = adapterPosition
            notifyDataSetChanged()

            onClick?.invoke(myDataset[selectedPosition])
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TextOptionViewHolder {
        val textView = WidgetItemView(parent.context)
        return TextOptionViewHolder(textView, onClick)
    }

    override fun onBindViewHolder(holder: TextOptionViewHolder, position: Int) {
        val item = myDataset[position]
        val itemIsSelected = selectedPosition == position
        val itemIsLast = myDataset.size - 1 == position

        holder.textItemView.setData(
            item,
            itemIsSelected,
            widgetType,
            correctOptionId,
            userSelectedOptionId,
            itemIsLast,
            component,
            fontFamilyProvider
        )
        if (showPercentage) {
            holder.textItemView.setProgressVisibility(showPercentage)
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = myDataset.size

    fun restoreSelectedPosition(optionId: String?) {
        optionId?.let { id ->
            selectedPosition = myDataset.indexOfFirst { it.id == id }
        }
    }
}
