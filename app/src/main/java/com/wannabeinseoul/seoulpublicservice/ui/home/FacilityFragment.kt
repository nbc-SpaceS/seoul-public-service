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
import com.wannabeinseoul.seoulpublicservice.databinding.FragmentFacilityBinding
import com.wannabeinseoul.seoulpublicservice.ui.main.MainViewModel
import com.wannabeinseoul.seoulpublicservice.ui.main.adapter.ItemAdapter

class FacilityFragment : Fragment() {
    private var _binding: FragmentFacilityBinding? = null
    private val binding get() = _binding!!

    private val regionPrefRepository by lazy { (requireActivity().application as SeoulPublicServiceApplication).container.regionPrefRepository }
    private val dbMemoryRepository by lazy { (requireActivity().application as SeoulPublicServiceApplication).container.dbMemoryRepository }
    private val mainViewModel: MainViewModel by activityViewModels { MainViewModel.factory }
    private val adapter by lazy {
        ItemAdapter(
            regionPrefRepository,
            "체육시설"
        ) { mainViewModel.moveSelectRegions(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentFacilityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var facilityItems = listOf(
            Item(R.drawable.ic_soccer, "축구장"),
            Item(R.drawable.ic_tennis, "테니스장"),
            Item(R.drawable.ic_pingpong, "탁구장"),
            Item(R.drawable.ic_golf, "골프장"),
            Item(R.drawable.ic_baseball, "야구장"),
            Item(R.drawable.ic_volleyball, "배구장"),
            Item(R.drawable.ic_footvolleyball, "족구장"),
            Item(R.drawable.ic_futsal, "풋살장"),
            Item(R.drawable.ic_badminton, "배드민턴장"),
            Item(R.drawable.ic_stadium, "다목적경기장"),
            Item(R.drawable.ic_gym, "체육관"),
            Item(R.drawable.ic_basketball, "농구장"),
        )
        ItemRepository.setItems("Facility", facilityItems)

        binding.rvFacility.adapter = adapter
        binding.rvFacility.layoutManager = GridLayoutManager(requireContext(), 4)
        adapter.submitList(facilityItems)

        mainViewModel.selectRegion.observe(viewLifecycleOwner) { region ->
            facilityItems = if (region != "지역선택") {
                facilityItems.map { item ->
                    val size = dbMemoryRepository.getFiltered(
                        areanm = listOf(region.toString()),
                        minclassnm = listOf(item.name)
                    ).size
                    item.copy(count = size)
                }
            } else {
                facilityItems.map { item ->
                    val size = dbMemoryRepository.getFiltered(minclassnm = listOf(item.name)).size
                    item.copy(count = size)
                }
            }

            binding.clFacilityNothing.isVisible = facilityItems.all { it.count == 0 }
            adapter.submitList(facilityItems)
        }
    }

}