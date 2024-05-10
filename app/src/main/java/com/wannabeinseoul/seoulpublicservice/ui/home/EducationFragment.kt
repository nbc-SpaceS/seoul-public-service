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
import com.wannabeinseoul.seoulpublicservice.databinding.FragmentEducationBinding
import com.wannabeinseoul.seoulpublicservice.ui.main.MainViewModel
import com.wannabeinseoul.seoulpublicservice.ui.main.adapter.ItemAdapter

class EducationFragment : Fragment() {
    private var _binding: FragmentEducationBinding? = null
    private val binding get() = _binding!!

    private val regionPrefRepository by lazy { (requireActivity().application as SeoulPublicServiceApplication).container.regionPrefRepository }
    private val dbMemoryRepository by lazy { (requireActivity().application as SeoulPublicServiceApplication).container.dbMemoryRepository }
    private val mainViewModel: MainViewModel by activityViewModels { MainViewModel.factory }
    private val adapter by lazy { ItemAdapter(regionPrefRepository, "교육강좌") { mainViewModel.moveSelectRegions(it) } }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        _binding = FragmentEducationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var educationItems = listOf(
            Item(R.drawable.ic_book, "교양/어학"),
            Item(R.drawable.ic_information, "정보통신"),
            Item(R.drawable.ic_history, "역사"),
            Item(R.drawable.ic_science, "자연/과학"),
            Item(R.drawable.ic_village, "도시농업"),
            Item(R.drawable.ic_contact, "청년정보"),
            Item(R.drawable.ic_sports, "스포츠"),
            Item(R.drawable.ic_art,  "미술제작"),
            Item(R.drawable.ic_knitting, "공예/취미"),
            Item(R.drawable.ic_certification, "전문/자격증"),
            Item(R.drawable.ic_etc, "기타"),
        )
        ItemRepository.setItems("Education", educationItems)

        binding.rvEducation.adapter = adapter
        binding.rvEducation.layoutManager = GridLayoutManager(requireContext(), 4)
        adapter.submitList(educationItems)

        mainViewModel.selectRegion.observe(viewLifecycleOwner) { region ->
            if (region != "지역선택") {
                educationItems = educationItems.map { item ->
                    val size = dbMemoryRepository.getFiltered(
                        areanm = listOf(region.toString()),
                        minclassnm = listOf(item.name)
                    ).size
                    item.copy(count = size)
                }
            } else {
                educationItems = educationItems.map { item ->
                    val size = dbMemoryRepository.getFiltered(
                        minclassnm = listOf(item.name)
                    ).size
                    item.copy(count = size)
                }
            }

            binding.clEducationNothing.isVisible = educationItems.all { it.count == 0 }
            adapter.submitList(educationItems)
        }
    }
}