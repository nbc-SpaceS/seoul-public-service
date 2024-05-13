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
import com.wannabeinseoul.seoulpublicservice.databinding.FragmentCultureEventBinding
import com.wannabeinseoul.seoulpublicservice.ui.category.CategoryFragment2
import com.wannabeinseoul.seoulpublicservice.ui.main.MainViewModel
import com.wannabeinseoul.seoulpublicservice.ui.main.adapter.ItemAdapter

class CultureEventFragment : Fragment() {
    private var _binding: FragmentCultureEventBinding? = null
    private val binding get() = _binding!!

    private val regionPrefRepository by lazy { (requireActivity().application as SeoulPublicServiceApplication).container.regionPrefRepository }
    private val dbMemoryRepository by lazy { (requireActivity().application as SeoulPublicServiceApplication).container.dbMemoryRepository }
    private val mainViewModel: MainViewModel by activityViewModels { MainViewModel.factory }
    private val adapter by lazy {
        ItemAdapter(
            regionPrefRepository,
            "문화체험",
            moveCategoryPage = { category, region ->
                val dialog = CategoryFragment2.newInstance(category, region)
                dialog.show(requireActivity().supportFragmentManager, "Category")
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentCultureEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var cultureEventItems = listOf(
            Item(R.drawable.ic_exhibition, "전시/관람"),
            Item(R.drawable.ic_experience, "교육체험"),
            Item(R.drawable.ic_event, "문화행사"),
            Item(R.drawable.ic_trekking, "산림여가"),
            Item(R.drawable.ic_park, "공원탐방"),
            Item(R.drawable.ic_kids, "서울형키즈카페"),
            Item(R.drawable.ic_farm, "농장체험"),
        )
        ItemRepository.setItems("CultureEvent", cultureEventItems)

        binding.rvCultureEvent.adapter = adapter
        binding.rvCultureEvent.layoutManager = GridLayoutManager(requireContext(), 4)
        adapter.submitList(cultureEventItems)

        mainViewModel.selectRegion.observe(viewLifecycleOwner) { region ->
            if (region != "지역선택") {
                cultureEventItems = cultureEventItems.map { item ->
                    val size = dbMemoryRepository.getFiltered(
                        areanm = listOf(region.toString()),
                        minclassnm = listOf(item.name)
                    ).size
                    item.copy(count = size)
                }

                binding.clCultureEventNothing.isVisible =
                    cultureEventItems.all { it.count == 0 }
                adapter.submitList(cultureEventItems)
            }
        }
    }
}