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
import com.wannabeinseoul.seoulpublicservice.databinding.FragmentMedicalBinding
import com.wannabeinseoul.seoulpublicservice.ui.category.CategoryFragment2
import com.wannabeinseoul.seoulpublicservice.ui.main.MainViewModel
import com.wannabeinseoul.seoulpublicservice.ui.main.adapter.ItemAdapter

class MedicalFragment : Fragment() {
    private var _binding: FragmentMedicalBinding? = null
    private val binding get() = _binding!!

    private val regionPrefRepository by lazy { (requireActivity().application as SeoulPublicServiceApplication).container.regionPrefRepository }
    private val dbMemoryRepository by lazy { (requireActivity().application as SeoulPublicServiceApplication).container.dbMemoryRepository }
    private val mainViewModel: MainViewModel by activityViewModels { MainViewModel.factory }
    private val adapter by lazy {
        ItemAdapter(
            regionPrefRepository,
            "진료복지",
            moveCategoryPage = { category, region ->
                val dialog = CategoryFragment2.newInstance(category, region)
                dialog.show(requireActivity().supportFragmentManager, "Category")
            }
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        _binding = FragmentMedicalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var medicalItems = listOf(
            Item(R.drawable.ic_hospital, "병원"),
            Item(R.drawable.ic_bus, "장애인버스"),
            Item(R.drawable.ic_childhospital, "어린이병원"),
        )
        ItemRepository.setItems("Medical", medicalItems)

        binding.rvMedical.adapter = adapter
        binding.rvMedical.layoutManager = GridLayoutManager(requireContext(), 4)
        adapter.submitList(medicalItems)

        mainViewModel.selectRegion.observe(viewLifecycleOwner) { region ->
            if (region != "지역선택") {
                medicalItems = medicalItems.map { item ->
                    val size = dbMemoryRepository.getFiltered(
                        areanm = listOf(region.toString()),
                        minclassnm = if (item.name == "병원") listOf("서북병원") else listOf(item.name)
                    ).size
                    item.copy(count = size)
                }
            } else {
                medicalItems = medicalItems.map { item ->
                    val size = dbMemoryRepository.getFiltered(
                        minclassnm = if (item.name == "병원") listOf("서북병원") else listOf(item.name)
                    ).size
                    item.copy(count = size)
                }
            }

            binding.clMedicalNothing.isVisible = medicalItems.all { it.count == 0 }
            adapter.submitList(medicalItems)
        }
    }
}