package com.wannabeinseoul.seoulpublicservice.ui.dialog.review

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.wannabeinseoul.seoulpublicservice.R
import com.wannabeinseoul.seoulpublicservice.databinding.FragmentReviewBinding
import com.wannabeinseoul.seoulpublicservice.ui.dialog.complaint.ComplaintFragment
import com.wannabeinseoul.seoulpublicservice.ui.main.MainViewModel

class ReviewFragment: BottomSheetDialogFragment() {

    private var _binding: FragmentReviewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReviewViewModel by viewModels { ReviewViewModel.factory }
    private val mainViewModel: MainViewModel by activityViewModels()

    private val adapter: ReviewAdapter by lazy {
        ReviewAdapter(
            complaintUser = {
                viewModel.checkComplaintSelf(it)
            },
            deleteReview = {
                viewModel.deleteReview(svcId, it)
                viewModel.checkWritableUser(svcId)
            },
            userId = viewModel.userId
        )
    }

    private val svcId by lazy {
        mainViewModel.selectedServiceId
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.AppBottomSheetDialogTheme)
        bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheetDialog.behavior.isDraggable = false
        return bottomSheetDialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initView()
        initViewModel()
    }

    private fun initView() = with(binding) {

        isCancelable = true

        rvReviewList.adapter = adapter

        dialog?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                dismiss()
                return@setOnKeyListener true
            }
            false
        }
    }

    private fun initViewModel() = with(viewModel) {
        val storedReviewList = mainViewModel.currentReviewList
        if (storedReviewList.isNotEmpty()) {
            setReviewsByList(storedReviewList)
        }

        checkWritableUser(svcId)

        uiState.observe(viewLifecycleOwner) {
            adapter.submitList(it.toList()) {
                binding.rvReviewList.smoothScrollToPosition(0)
            }
            binding.tvReviewCount.text = it.size.toString()
            if (it.isNotEmpty()) {
                binding.tvReviewCount.isVisible = true
                binding.tvReviewEmptyDescription.isVisible = false
            } else {
                binding.tvReviewCount.isVisible = false
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.tvReviewEmptyDescription.isVisible = true
                }, 300)
            }
        }

        reviewCredentials.observe(viewLifecycleOwner) {
            if (it) {
                binding.etReviewInputField.hint = "후기를 입력해주세요."
                binding.ivReviewSendBtn.setImageResource(R.drawable.ic_send)
                binding.ivReviewSendBtn.setOnClickListener {
                    if (binding.etReviewInputField.text.isNotEmpty()) {
                        viewModel.uploadReview(svcId, binding.etReviewInputField.text.toString())
                        setInitialState()
                    }
                }

                binding.etReviewInputField.setOnEditorActionListener { _, action, _ ->
                    if (action == EditorInfo.IME_ACTION_SEARCH) {
                        if (binding.etReviewInputField.text.isNotEmpty()) {
                            viewModel.uploadReview(svcId, binding.etReviewInputField.text.toString())
                            setInitialState()
                        }
                    }

                    false
                }
            } else {
                binding.etReviewInputField.hint = "작성한 후기 수정만 가능합니다."
                binding.ivReviewSendBtn.setImageResource(R.drawable.ic_revise)
                binding.ivReviewSendBtn.setOnClickListener {
                    if (binding.etReviewInputField.text.isNotEmpty()) {
                        viewModel.reviseReview(svcId, binding.etReviewInputField.text.toString())
                        setInitialState()
                    }
                }

                binding.etReviewInputField.setOnEditorActionListener { _, action, _ ->
                    if (action == EditorInfo.IME_ACTION_SEARCH) {
                        if (binding.etReviewInputField.text.isNotEmpty()) {
                            viewModel.reviseReview(svcId, binding.etReviewInputField.text.toString())
                            setInitialState()
                        }
                    }

                    false
                }
            }
        }

        isComplaintSelf.observe(viewLifecycleOwner) {
            if (it.first) {
                Toast.makeText(requireContext(), "스스로를 신고할 수는 없습니다.", Toast.LENGTH_SHORT).show()
            } else {
                val complaintFragment = ComplaintFragment.newInstance()
                mainViewModel.setComplaintUserInfo(it.second)
                complaintFragment.show(requireActivity().supportFragmentManager, "Complaint")
            }
        }

        mainViewModel.refreshReviewListState.observe(viewLifecycleOwner) {
            setReviews(svcId)
        }
    }

    private fun setInitialState() {
        binding.etReviewInputField.setText("")
        binding.etReviewInputField.clearFocus()

        val inputMethodManager =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(
            binding.etReviewInputField.windowToken,
            0
        )
    }

    override fun onDismiss(dialog: DialogInterface) {
        viewModel.uiState.value?.let { mainViewModel.setCurrentReviewList(it) }
        mainViewModel.setReviewListState(true)
        super.onDismiss(dialog)
    }
}