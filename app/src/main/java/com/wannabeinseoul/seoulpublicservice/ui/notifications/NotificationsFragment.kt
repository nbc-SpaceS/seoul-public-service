package com.wannabeinseoul.seoulpublicservice.ui.notifications

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.wannabeinseoul.seoulpublicservice.R
import com.wannabeinseoul.seoulpublicservice.databinding.FragmentNotificationsBinding
import com.wannabeinseoul.seoulpublicservice.ui.detail.DetailCloseInterface
import com.wannabeinseoul.seoulpublicservice.ui.detail.DetailFragment
import com.wannabeinseoul.seoulpublicservice.ui.main.MainViewModel

class NotificationsFragment : DialogFragment() {

    companion object {
        fun newInstance() = NotificationsFragment()
    }

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotificationsViewModel by viewModels { NotificationsViewModel.factory }
    private val mainViewModel: MainViewModel by activityViewModels { MainViewModel.factory }

    private val adapter: NotificationAdapter by lazy {
        NotificationAdapter(
            moveDetail = {
                val dialog = DetailFragment.newInstance(it)
                dialog.setCloseListener(object : DetailCloseInterface {
                    override fun onDialogClosed() {
                        mainViewModel.setMappingData()
                    }
                })
                dialog.show(requireActivity().supportFragmentManager, "Detail")
                dismiss()
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext(), R.style.DetailTransparent)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initViewModel()
    }

    private fun initView() = with(binding) {
        ivNotificationBackBtn.setOnClickListener {
            dismiss()
        }

        rvNotification.adapter = adapter

        viewModel.updateUiState()
    }

    private fun initViewModel() = with(viewModel) {
        uiState.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

    }
}
