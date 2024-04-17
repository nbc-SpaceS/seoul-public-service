package com.wannabeinseoul.seoulpublicservice.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.wannabeinseoul.seoulpublicservice.R
import com.wannabeinseoul.seoulpublicservice.SeoulPublicServiceApplication
import com.wannabeinseoul.seoulpublicservice.data.Item
import com.wannabeinseoul.seoulpublicservice.data.ItemRepository
import com.wannabeinseoul.seoulpublicservice.databinding.FragmentFacilityRentBinding
import com.wannabeinseoul.seoulpublicservice.ui.main.MainViewModel
import com.wannabeinseoul.seoulpublicservice.ui.main.adapter.ItemAdapter

class FacilityRentFragment : Fragment() {
    private var _binding: FragmentFacilityRentBinding? = null
    private val binding get() = _binding!!

    private val regionPrefRepository by lazy { (requireActivity().application as SeoulPublicServiceApplication).container.regionPrefRepository }
    private val dbMemoryRepository by lazy { (requireActivity().application as SeoulPublicServiceApplication).container.dbMemoryRepository }
    private val mainViewModel: MainViewModel by activityViewModels()
    private val adapter by lazy { ItemAdapter(regionPrefRepository, "공간시설") { mainViewModel.moveSelectRegions(it) } }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        _binding = FragmentFacilityRentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var facilityRentItems = listOf(
            Item(R.drawable.ic_door, "다목적실"),
            Item(R.drawable.ic_concert, "공연장"),
            Item(R.drawable.ic_auditorium, "강당"),
            Item(R.drawable.ic_neighbor, "주민공유공간"),
            Item(R.drawable.ic_camping, "캠핑장"),
            Item(R.drawable.ic_room, "청년공간"),
            Item(R.drawable.ic_record, "녹화장소"),
            Item(R.drawable.ic_meeting, "회의실"),
            Item(R.drawable.ic_lecture, "강의실"),
            Item(R.drawable.ic_etc, "민원/기타"),
        )
        ItemRepository.setItems("FacilityRent", facilityRentItems)

        binding.rvFacilityRent.adapter = adapter
        binding.rvFacilityRent.layoutManager = GridLayoutManager(requireContext(), 4)
        adapter.submitList(facilityRentItems)

        // 지역 선택 시 해당 지역에 있는 시설물의 개수를 가져와서 갱신
        mainViewModel.selectRegion.observe(viewLifecycleOwner) { region ->
            if (region != "지역선택") {
                facilityRentItems = facilityRentItems.map { item ->
                    val size = dbMemoryRepository.getFiltered(
                        areanm = listOf(region.toString()),
                        minclassnm = listOf(item.name)
                    ).size
                    item.copy(count = size)
                }
            } else {
                facilityRentItems = facilityRentItems.map { item ->
                    val size = dbMemoryRepository.getFiltered(
                        minclassnm = listOf(item.name)
                    ).size
                    item.copy(count = size)
                }
            }

            binding.clFacilityRentNothing.isVisible = facilityRentItems.all { it.count == 0 }
            adapter.submitList(facilityRentItems)
        }
    }
}