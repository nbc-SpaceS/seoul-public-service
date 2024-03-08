package com.wannabeinseoul.seoulpublicservice.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.wannabeinseoul.seoulpublicservice.databinding.FragmentNotificationsBinding
import com.wannabeinseoul.seoulpublicservice.detail.DetailFragment

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NotificationsViewModel by viewModels { NotificationsViewModel.factory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initViewModel()
    }

    private fun initView() = binding.let { b ->
        b.btnTemp1.setOnClickListener { viewModel.setRandomOne() }
        b.tvTemp1.setOnClickListener {
            val dialog = DetailFragment.newInstance("S240104091254073361")
            dialog.show(requireActivity().supportFragmentManager, "Detail")
        }
    }

    private fun initViewModel() = viewModel.let { vm ->
        vm.text.observe(viewLifecycleOwner) {
            binding.tvTemp1.text = it
        }
        vm.isBtnEnabled.observe(viewLifecycleOwner) {
            binding.btnTemp1.isEnabled = it
        }
    }
}
