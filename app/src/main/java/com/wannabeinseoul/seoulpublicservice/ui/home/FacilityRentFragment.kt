package com.wannabeinseoul.seoulpublicservice.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.wannabeinseoul.seoulpublicservice.R
import com.wannabeinseoul.seoulpublicservice.SeoulPublicServiceApplication
import com.wannabeinseoul.seoulpublicservice.adapter.ItemAdapter
import com.wannabeinseoul.seoulpublicservice.data.Item
import com.wannabeinseoul.seoulpublicservice.data.ItemRepository
import com.wannabeinseoul.seoulpublicservice.databinding.FragmentFacilityRentBinding

class FacilityRentFragment : Fragment() {
    private var _binding: FragmentFacilityRentBinding? = null
    private val binding get() = _binding!!

    private val regionPrefRepository by lazy { (requireActivity().application as SeoulPublicServiceApplication).container.regionPrefRepository }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        _binding = FragmentFacilityRentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val facilityRentItems = listOf(
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

        val items = ItemRepository.getItems("FacilityRent")
        val adapter = ItemAdapter(items, regionPrefRepository)
        binding.rvFacilityRent.adapter = adapter
        binding.rvFacilityRent.layoutManager = GridLayoutManager(requireContext(), 4)
//        val homeFragment = parentFragment as HomeFragment
//        val selectedRegion = homeFragment.settingRegions()
//        Log.d("FacilityRentFragment", "Loaded selected region: $selectedRegion")
//        val adapter = ItemAdapter(items, selectedRegion)
//        binding.rvFacilityRent.adapter = adapter
//        binding.rvFacilityRent.layoutManager = GridLayoutManager(requireContext(), 4)
//        regionPrefRepository.selectedRegion().observe(viewLifecycleOwner) { selectedRegion ->
//            Log.d("FacilityRentFragment", "Loaded selected region: $selectedRegion")
//            val adapter = ItemAdapter(items, selectedRegion)
//            binding.rvFacilityRent.adapter = adapter
//            binding.rvFacilityRent.layoutManager = GridLayoutManager(requireContext(), 4)
//        }
    }
}