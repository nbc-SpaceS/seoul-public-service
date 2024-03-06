package com.wannabeinseoul.seoulpublicservice.ui.recommendation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.wannabeinseoul.seoulpublicservice.databinding.RecommendationItemBinding

class RecommendationHorizontalAdapter(
    private var items: MutableList<RecommendationData>,
    private val onItemClick: (svcid: String) -> Unit,
) : RecyclerView.Adapter<RecommendationHorizontalAdapter.VH>() {

    inner class VH(private val b: RecommendationItemBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun onBind(item: RecommendationData) {


            b.ivRcSmallImage.load(item.imageUrl)
            b.tvRcPlaceName.text = item.placeName
            b.tvRcPayType.text = item.payType
            b.tvRcAreaName.text = item.areaName
//            b.tvRcIsReservationAvailable.text = if (item.svcstatnm) {
//                "예약가능"
//            } else {
//                "예약종료"
//            }
//        binding.tvRcReview.text = "후기 ${recommendation.reviewCount}개"
            //추가 할 예정.

            b.root.setOnClickListener { onItemClick(item.svcid) }

        }
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(
            RecommendationItemBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.onBind(items[position])
    }

}
