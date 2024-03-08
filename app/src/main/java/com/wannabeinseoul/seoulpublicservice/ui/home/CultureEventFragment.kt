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
import com.wannabeinseoul.seoulpublicservice.databinding.FragmentCultureEventBinding

class CultureEventFragment : Fragment() {
    private var _binding: FragmentCultureEventBinding? = null
    private val binding get() = _binding!!

    private val regionPrefRepository by lazy { (requireActivity().application as SeoulPublicServiceApplication).container.regionPrefRepository }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        _binding = FragmentCultureEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cultureEventItems = listOf(
            Item(R.drawable.ic_exhibition, "전시/관람"),
            Item(R.drawable.ic_experience, "교육체험"),
            Item(R.drawable.ic_event, "문화행사"),
            Item(R.drawable.ic_trekking, "산림여가"),
            Item(R.drawable.ic_park, "공원탐방"),
            Item(R.drawable.ic_kids, "서울형키즈카페"),
            Item(R.drawable.ic_farm, "농장체험"),
        )
        ItemRepository.setItems("CultureEvent", cultureEventItems)

        val items = ItemRepository.getItems("CultureEvent")
        val adapter = ItemAdapter(items, regionPrefRepository)
        binding.rvCultureEvent.adapter = adapter
        binding.rvCultureEvent.layoutManager = GridLayoutManager(requireContext(), 4)

//        val homeFragment = parentFragment as HomeFragment
//        val selectedRegion = homeFragment.settingRegions()
//        Log.d("CultureEventFragment", "Loaded selected region: $selectedRegion")
//        val adapter = ItemAdapter(items, selectedRegion)
//        binding.rvCultureEvent.adapter = adapter
//        binding.rvCultureEvent.layoutManager = GridLayoutManager(requireContext(), 4)
//        regionPrefRepository.selectedRegion().observe(viewLifecycleOwner) { selectedRegion ->
//            Log.d("CultureEventFragment", "Loaded selected region: $selectedRegion")
//            val adapter = ItemAdapter(items, selectedRegion)
//            binding.rvCultureEvent.adapter = adapter
//            binding.rvCultureEvent.layoutManager = GridLayoutManager(requireContext(), 4)
//        }
    }
}