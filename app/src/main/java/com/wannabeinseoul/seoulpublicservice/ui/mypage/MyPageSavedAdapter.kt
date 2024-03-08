package com.wannabeinseoul.seoulpublicservice.ui.mypage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.wannabeinseoul.seoulpublicservice.R
import com.wannabeinseoul.seoulpublicservice.databinding.CategoryItemBinding
import com.wannabeinseoul.seoulpublicservice.databinding.RecommendationItemBinding
import com.wannabeinseoul.seoulpublicservice.seoul.Row
import com.wannabeinseoul.seoulpublicservice.util.loadWithHolder

class MyPageSavedAdapter(
    private val onSavedClick: (svcid: String) -> Unit,
) : ListAdapter<Row?, MyPageSavedAdapter.VH>(
    object : DiffUtil.ItemCallback<Row?>() {
        // TODO: null로 하려니까 변경 시 애니메이션이 이상해진다. 따로 데이터 클래스 만들어서 써야할듯. 서비스아이디랑 같이.
        override fun areItemsTheSame(oldItem: Row, newItem: Row): Boolean =
            oldItem.svcid == newItem.svcid

        override fun areContentsTheSame(oldItem: Row, newItem: Row): Boolean =
            oldItem == newItem
    }
) {

    inner class VH(private val b: RecommendationItemBinding) :
        RecyclerView.ViewHolder(b.root) {

        init {
            b.root.setOnClickListener {}
        }

        fun onBind(item: Row?) {
            if (item == null) {
                // TODO: 레이아웃에서 '삭제된 서비스입니다' 띄우는거 겹쳐놓고 gone으로 놨다가 띄워야 할 듯.

                b.ivRcSmallImage.load(R.drawable.place_holder_1)
                b.tvRcPlaceName.text = null
                b.tvRcPayType.text = null
                b.tvRcAreaName.text = null
                b.tvRcIsReservationAvailable.text = null
            } else {
                b.ivRcSmallImage.loadWithHolder(item.imgurl)
                b.tvRcPlaceName.text = item.svcnm
                b.tvRcPayType.text = item.payatnm
                b.tvRcAreaName.text = item.areanm
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
