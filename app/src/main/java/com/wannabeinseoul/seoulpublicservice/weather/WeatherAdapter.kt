package com.wannabeinseoul.seoulpublicservice.weather


import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wannabeinseoul.seoulpublicservice.R
import com.wannabeinseoul.seoulpublicservice.databinding.ItemHomeWeatherBinding
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class WeatherAdapter : ListAdapter<WeatherShort, WeatherAdapter.Holder>(object :
    DiffUtil.ItemCallback<WeatherShort>() {
    override fun areItemsTheSame(oldItem: WeatherShort, newItem: WeatherShort): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: WeatherShort, newItem: WeatherShort): Boolean {
        return oldItem == newItem
    }
}) {
    val today = LocalDate.now()!!

    inner class Holder(val binding: ItemHomeWeatherBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(dto: WeatherShort) {
            Log.i("This is WeatherAdapter", "dto : ${dto.sky}")
            binding.ivHomeWeatherIcon.setImageResource(
                when (dto.sky) {
                    1 -> R.drawable.face_sunny
                    3 -> R.drawable.face_cloudy
                    4 -> {
                        when {
                            dto.pop >= 60 -> R.drawable.face_rainy
                            dto.pop < 60 -> R.drawable.face_cloudy_dark
                            else -> throw Exception()
                        }
                    }

                    else -> throw Exception()
                }
            )
            binding.tvHomeWeatherPop.text = "${dto.pop}%"
            binding.tvHomeWeatherTmp.text = "${dto.tmp} ℃"
        }

        fun day(pos: Int) {
            val now = today.plusDays(pos.toLong())
            binding.tvHomeWeatherDay.text = when {
                now.dayOfMonth == 1 -> {
                    when (now.monthValue) {
                        1 -> "${now.year}.${now.monthValue}.${now.dayOfMonth}${
                            now.dayOfWeek.getDisplayName(
                                TextStyle.FULL,
                                Locale.KOREA
                            ).replace("요일", "")
                        })"

                        else -> "${now.monthValue}.${now.dayOfMonth}(${
                            now.dayOfWeek.getDisplayName(
                                TextStyle.FULL,
                                Locale.KOREA
                            ).replace("요일", "")
                        })"
                    }
                }

                else -> "${now.dayOfMonth}(${
                    now.dayOfWeek.getDisplayName(
                        TextStyle.FULL,
                        Locale.KOREA
                    ).replace("요일", "")
                })"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding =
            ItemHomeWeatherBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        Log.i("This is WeatherAdapter", "getItem : ${getItem(position)}")
        holder.bind(getItem(position))
        holder.day(position)
    }
}