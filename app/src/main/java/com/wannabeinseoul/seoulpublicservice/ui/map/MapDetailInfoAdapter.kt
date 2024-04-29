package com.wannabeinseoul.seoulpublicservice.ui.map

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wannabeinseoul.seoulpublicservice.R
import com.wannabeinseoul.seoulpublicservice.databinding.ItemMapInfoWindowBinding
import com.wannabeinseoul.seoulpublicservice.pref.SavedPrefRepository
import com.wannabeinseoul.seoulpublicservice.util.loadWithHolder

class MapDetailInfoAdapter(
    private val saveService: (String) -> Unit,
    private val moveReservationPage: (String) -> Unit,
    private val shareUrl: (String) -> Unit,
    private val moveDetailPage: (String) -> Unit,
    private val savedPrefRepository: SavedPrefRepository
) : ListAdapter<DetailInfoWindow, MapDetailInfoAdapter.InfoViewHolder>(object : DiffUtil.ItemCallback<DetailInfoWindow>() {
    override fun areItemsTheSame(oldItem: DetailInfoWindow, newItem: DetailInfoWindow): Boolean {
        return oldItem.svcid == newItem.svcid
    }

    override fun areContentsTheSame(oldItem: DetailInfoWindow, newItem: DetailInfoWindow): Boolean {
        return oldItem == newItem
    }

}) {
    abstract class InfoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        abstract fun onBind(item: DetailInfoWindow)
    }

    override fun getItemViewType(position: Int): Int {
        val info = getItem(position)
        return if (info is DetailInfoWindow) {
            0
        } else {
            1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InfoViewHolder {
        return when (viewType) {
            0 -> {
                DetailInfoViewHolder(
                    binding = ItemMapInfoWindowBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    ),
                    saveService = saveService,
                    moveReservationPage = moveReservationPage,
                    shareUrl = shareUrl,
                    moveDetailPage = moveDetailPage,
                    savedPrefRepository = savedPrefRepository
                )
            }

            else -> {
                UnknownInfoViewHolder(
                    binding = ItemMapInfoWindowBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
        }
    }

    override fun onBindViewHolder(holder: InfoViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }

    class DetailInfoViewHolder(
        private val binding: ItemMapInfoWindowBinding,
        private val saveService: (String) -> Unit,
        private val moveReservationPage: (String) -> Unit,
        private val shareUrl: (String) -> Unit,
        private val moveDetailPage: (String) -> Unit,
        private val savedPrefRepository: SavedPrefRepository
    ) : InfoViewHolder(binding.root) {
        override fun onBind(item: DetailInfoWindow) = with(binding) {
            if (item.saved) {
                ivMapInfoSaveServiceBtn.setImageResource(R.drawable.ic_save_fill)
                ivMapInfoSaveServiceBtn.drawable.setTint(Color.parseColor("#F8496C"))
            } else {
                ivMapInfoSaveServiceBtn.setImageResource(R.drawable.ic_save_empty)
            }
            ivMapInfoPicture.loadWithHolder(item.imgurl)
            tvMapInfoRegion.text = item.areanm
            tvMapInfoService.text =
                HtmlCompat.fromHtml(item.svcnm, HtmlCompat.FROM_HTML_MODE_LEGACY)
            tvMapInfoPay.text = item.payatnm
            btnMapInfoReservation.text = when (item.svcstatnm) {
                "안내중" -> {
                    "예약안내"
                }

                "접수중" -> {
                    "예약하기"
                }

                else -> {
                    item.svcstatnm
                }
            }

            binding.btnMapInfoReservation.isEnabled = when (item.svcstatnm) {
                "안내중", "접수중" -> {
                    true
                }

                else -> {
                    false
                }
            }

            binding.ivMapInfoSaveServiceBtn.setOnClickListener {
                saveService(item.svcid).let {
                    if (savedPrefRepository.contains(item.svcid)) {
                        ivMapInfoSaveServiceBtn.setImageResource(R.drawable.ic_save_fill)
                        ivMapInfoSaveServiceBtn.drawable.setTint(Color.parseColor("#F8496C"))
                    } else {
                        ivMapInfoSaveServiceBtn.setImageResource(R.drawable.ic_save_empty)
                    }
                }
            }

            binding.btnMapInfoReservation.setOnClickListener {
                moveReservationPage(item.svcurl)
            }

            binding.ivMapInfoShareBtn.setOnClickListener {
                shareUrl(item.svcurl)
            }

            binding.clMapInfoWindow.setOnClickListener {
                moveDetailPage(item.svcid)
            }
        }
    }

    class UnknownInfoViewHolder(
        private val binding: ItemMapInfoWindowBinding
    ) : InfoViewHolder(binding.root) {
        override fun onBind(item: DetailInfoWindow) = Unit
    }
}