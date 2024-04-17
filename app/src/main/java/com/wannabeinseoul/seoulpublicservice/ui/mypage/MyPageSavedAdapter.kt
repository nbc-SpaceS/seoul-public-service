package com.wannabeinseoul.seoulpublicservice.ui.mypage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.wannabeinseoul.seoulpublicservice.R
import com.wannabeinseoul.seoulpublicservice.databinding.RecommendationItemBinding
import com.wannabeinseoul.seoulpublicservice.ui.recommendation.RecommendationData
import com.wannabeinseoul.seoulpublicservice.util.fromHtml
import com.wannabeinseoul.seoulpublicservice.util.loadWithHolder

class MyPageSavedAdapter(
    private val onSavedClick: (svcid: String) -> Unit,
) : ListAdapter<RecommendationData?, MyPageSavedAdapter.VH>(
    object : DiffUtil.ItemCallback<RecommendationData?>() {
        // TODO: null로 하려니까 변경 시 애니메이션이 이상해진다. 따로 데이터 클래스 만들어서 써야할듯. 서비스아이디랑 같이.
        override fun areItemsTheSame(
            oldItem: RecommendationData,
            newItem: RecommendationData
        ): Boolean =
            oldItem.svcid == newItem.svcid

        override fun areContentsTheSame(
            oldItem: RecommendationData,
            newItem: RecommendationData
        ): Boolean =
            oldItem == newItem
    }
) {

    inner class VH(private val b: RecommendationItemBinding) :
        RecyclerView.ViewHolder(b.root) {

        init {
            b.root.setOnClickListener {}
        }

        fun onBind(item: RecommendationData?) {
            if (item == null) {
                // TODO: 레이아웃에서 '삭제된 서비스입니다' 띄우는거 겹쳐놓고 gone으로 놨다가 띄워야 할 듯.

                b.ivRcSmallImage.load(R.drawable.place_holder_1)
                b.tvRcPlaceName.text = null
                b.tvRcPayType.text = null
                b.tvRcAreaName.text = null
                b.tvRcIsReservationAvailable.text = null
            } else {
                b.ivRcSmallImage.loadWithHolder(item.imageUrl)
                b.tvRcPlaceName.text = item.serviceName.fromHtml()
                b.tvRcPayType.text = item.payType.take(2)
                b.tvRcAreaName.text = item.areaName
                b.tvRcIsReservationAvailable.text = item.svcstatnm

                b.root.setOnClickListener { onSavedClick(item.svcid) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(
            RecommendationItemBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.onBind(getItem(position))
    }
}
