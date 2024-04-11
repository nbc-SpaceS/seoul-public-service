package com.wannabeinseoul.seoulpublicservice.ui.dialog.setting

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.wannabeinseoul.seoulpublicservice.databinding.DialogSettingBinding
import com.wannabeinseoul.seoulpublicservice.ui.main.MainViewModel

class SettingDialog: DialogFragment() {

    companion object {
        fun newInstance() = SettingDialog()
    }

    private var _binding: DialogSettingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingViewModel by viewModels { SettingViewModel.factory }
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSettingBinding.inflate(inflater, container, false)

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
        initViewModel()
    }

    private fun initView() = with(binding) {

        btnSettingSynchronizationUpload.setOnClickListener {
            btnSettingSynchronizationUpload.isEnabled = false
            clSettingSynchronizationKey.isVisible = false
            binding.pbSettingLoading.visibility = View.VISIBLE
            viewModel.storeDataToServer(mainViewModel.userName)
        }

        clSettingSynchronizationKey.setOnClickListener {
            val clipboardManager = requireContext().getSystemService(ClipboardManager::class.java)
            val clipKeyText = ClipData.newPlainText("label", tvSettingGottenKey.text)
            clipboardManager.setPrimaryClip(clipKeyText)

            Toast.makeText(requireContext(), "동기화 키가 클립보드에 저장되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initViewModel() = with(viewModel) {
       synchronizationKey.observe(viewLifecycleOwner) {
           binding.tvSettingGottenKey.text = it.ifEmpty { "데이터 저장에 실패했습니다." }
           binding.clSettingSynchronizationKey.isVisible = true
           binding.pbSettingLoading.visibility = View.INVISIBLE
           binding.btnSettingSynchronizationUpload.isEnabled = true

           storeKeyToPref(it)
       }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}