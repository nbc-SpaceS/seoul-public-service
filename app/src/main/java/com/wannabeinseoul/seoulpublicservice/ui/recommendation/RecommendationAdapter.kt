package com.wannabeinseoul.seoulpublicservice.ui.recommendation

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wannabeinseoul.seoulpublicservice.databinding.RecommendationItemRecommendedBinding
import com.wannabeinseoul.seoulpublicservice.databinding.RecommendationItemRecommendedTipBinding

class RecommendationAdapter :
    ListAdapter<RecommendationAdapter.MultiView, RecyclerView.ViewHolder>(DiffCallback()) {

    /** sealed interface */

    sealed interface MultiView {

        enum class Type {
            HORIZONTAL,
            TIP,
        }

        val viewType: Type

        data class Horizontal(
            val keyword: String,
            val headerTitle: String,
            val adapter: RecommendationHorizontalAdapter,
            val infiniteScrollLambdaFunc: (String, Int) -> Unit,
        ) : MultiView {
            override val viewType: Type = Type.HORIZONTAL
        }

        data class Tip(
            val title: String,
            val content: String,
        ) : MultiView {
            override val viewType: Type = Type.TIP
        }
    }

    /** 뷰홀더들 */

    inner class HorizontalViewHolder(
        private val binding: RecommendationItemRecommendedBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MultiView.Horizontal) {
            binding.rvShared.adapter = item.adapter
            binding.rvShared.itemAnimator = null
            binding.tvSharedText.text = item.headerTitle
            binding.rvShared.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    val lastVisiblePosition = (recyclerView.layoutManager as LinearLayoutManager)
                        .findLastCompletelyVisibleItemPosition()
                    val lastPosition = recyclerView.adapter!!.itemCount - 1

                    if (lastVisiblePosition == lastPosition) {
                        Log.d("dkj", "현재 개수 : ${recyclerView.adapter?.itemCount}")
                        item.infiniteScrollLambdaFunc(
                            item.keyword,
                            recyclerView.adapter?.itemCount?.plus(5) ?: 5
                        )
                    }
                }
            })
        }
    }

    inner class TipViewHolder(private val binding: RecommendationItemRecommendedTipBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MultiView.Tip) {
            binding.tvTipTitle.text = item.title
            binding.tvTipContent.text = item.content
        }
    }

    /* 뷰홀더 끝 */

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            MultiView.Type.HORIZONTAL.ordinal -> HorizontalViewHolder(
                RecommendationItemRecommendedBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)
            )

            MultiView.Type.TIP.ordinal -> TipViewHolder(
                RecommendationItemRecommendedTipBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)
            )

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (item.viewType) {
            MultiView.Type.HORIZONTAL -> (holder as HorizontalViewHolder).bind(item as MultiView.Horizontal)
            MultiView.Type.TIP -> (holder as TipViewHolder).bind(item as MultiView.Tip)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position).viewType.ordinal
    }

    class DiffCallback : DiffUtil.ItemCallback<MultiView>() {
        override fun areItemsTheSame(oldItem: MultiView, newItem: MultiView): Boolean {
            return if (oldItem is MultiView.Horizontal && newItem is MultiView.Horizontal) {
                oldItem.keyword == newItem.keyword
            } else if (oldItem is MultiView.Tip && newItem is MultiView.Tip) {
                oldItem.content == newItem.content
            } else {
                oldItem === newItem
            }
        }

        override fun areContentsTheSame(oldItem: MultiView, newItem: MultiView): Boolean {
            return oldItem == newItem
        }
    }
}