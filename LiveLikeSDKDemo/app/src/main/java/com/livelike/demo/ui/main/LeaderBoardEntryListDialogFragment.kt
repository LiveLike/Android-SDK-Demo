package com.livelike.demo.ui.main

import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.livelike.demo.R
import com.livelike.engagementsdk.core.data.models.LeaderBoard
import com.livelike.engagementsdk.core.data.models.LeaderBoardEntry
import com.livelike.engagementsdk.core.data.models.LeaderBoardEntryPaginationResult
import com.livelike.engagementsdk.publicapis.LiveLikeCallback


// TODO: Customize parameter argument names
const val ARG_ITEM_COUNT = "item_count"

/**
 *
 * A fragment that shows a list of items as a modal bottom sheet.
 *
 * You can show this modal bottom sheet from your activity like this:
 * <pre>
 *    LeaderBoardEntryListDialogFragment.newInstance(30).show(supportFragmentManager, "dialog")
 * </pre>
 */
class LeaderBoardEntryListDialogFragment : BottomSheetDialogFragment() {

    //private var _binding: FragmentLeaderboardBottomSheetBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    // private val binding get() = _binding!!
    private lateinit var pageViewModel: PageViewModel
    lateinit var list: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_leaderboard_bottom_sheet, container, false)
        list = root.findViewById(R.id.list)
        list.layoutManager = LinearLayoutManager(context)

        val itemDecorator = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        itemDecorator.setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.divider)!!)
        list.addItemDecoration(itemDecorator)

        /* val spacingInPixels = resources.getDimensionPixelSize(R.dimen.spacing)
         list.addItemDecoration(SpacesItemDecoration(spacingInPixels))*/




        return root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // val list: RecyclerView = view.findViewById(R.id.list)
        /*list.layoutManager = GridLayoutManager(context, 3)
        activity?.findViewById<RecyclerView>(R.id.list)?.adapter = LeaderBoardEntryAdapter(4)*/
        // arguments?.getInt(ARG_ITEM_COUNT)?.let {  }
        /* val dialog : Dialog? = getDialog() */

        /* val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet);
         val behavior = BottomSheetBehavior.from(bottomSheet!!);
         //behavior.setState(BottomSheetBehavior.SAVE_PEEK_HEIGHT);
         behavior.setPeekHeight(300)*/

        val offsetFromTop = 800
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            isFitToContents = false
            setExpandedOffset(offsetFromTop)
            state = BottomSheetBehavior.STATE_EXPANDED
        }

        pageViewModel = ViewModelProvider(requireActivity()).get(PageViewModel::class.java)
        pageViewModel.initiateLeaderBoard(object : LiveLikeCallback<List<LeaderBoard>>() {
            override fun onResponse(result: List<LeaderBoard>?, error: String?) {
                result?.let {
                    Log.e("", "" + result)
                    //get leaderboard details
                    //86ef1ca9-5ebf-4f8f-8a4b-a4a34697bc47
                    pageViewModel.getLeaderBoardEntries(object :
                        LiveLikeCallback<LeaderBoardEntryPaginationResult>() {
                        override fun onResponse(
                            result: LeaderBoardEntryPaginationResult?,
                            error: String?
                        ) {
                            result?.let {
                                result.list?.let {
                                    val adapter = LeaderBoardEntryAdapter()
                                    adapter.dataList = it
                                    list?.adapter = adapter
                                }

                            }
                            error?.let {
                                //showToast(error)
                            }
                        }
                    })

                }
                error?.let {

                }
            }
        })
    }

    private inner class ViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val rank: TextView = view.findViewById(R.id.rank)
        val name: TextView = view.findViewById(R.id.name)
        val pts: TextView = view.findViewById(R.id.pts)
    }

    private inner class LeaderBoardEntryAdapter internal constructor() :
        RecyclerView.Adapter<ViewHolder>() {

        lateinit var dataList: List<LeaderBoardEntry>
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

            return return ViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.fragment_leaderboard_entry_item_name, parent, false)
            )
            /*return when (viewType) {
                VIEW_TYPE_PTS -> return ViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.fragment_leaderboard_entry_item, parent, false)

                )

                VIEW_TYPE_RANK -> return ViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.fragment_leaderboard_entry_item_rank, parent, false)

                )

                VIEW_TYPE_NAME -> return ViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.fragment_leaderboard_entry_item_name, parent, false)

                )

                else -> ViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.fragment_leaderboard_entry_item, parent, false)
                )
            }*/


        }

        /*override fun getItemViewType(position: Int): Int {
            return when {
                position % 3 == 0 -> {
                    VIEW_TYPE_RANK
                }
                position % 3 == 1 -> {
                    VIEW_TYPE_NAME
                }
                else -> {
                    VIEW_TYPE_PTS
                }
            }

        }*/

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.rank.text = dataList?.get(position)?.rank?.toString()
            holder.name.text = dataList?.get(position)?.profile_nickname.toString()
            holder.pts.text = dataList?.get(position)?.score.toString()
        }

        override fun getItemCount(): Int {
            return dataList.size
        }

    }


    companion object {

        val TAG: String? = LeaderBoardEntryListDialogFragment.javaClass.name

        // TODO: Customize parameters
        fun newInstance(itemCount: Int): LeaderBoardEntryListDialogFragment =
            LeaderBoardEntryListDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ITEM_COUNT, itemCount)
                }
            }

    }

    internal class SpacesItemDecoration(private val space: Int) : ItemDecoration() {

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            outRect.left = space
            outRect.right = space
            outRect.top = space

            // Add top margin only for the first item to avoid double space between items
            if (parent.getChildLayoutPosition(view!!) % 3 == 1) {
                outRect.bottom = 0
            } else {
                outRect.bottom = space
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        //_binding = null
    }
}