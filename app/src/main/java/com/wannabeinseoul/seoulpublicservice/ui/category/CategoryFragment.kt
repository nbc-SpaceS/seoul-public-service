package com.wannabeinseoul.seoulpublicservice.ui.category

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.wannabeinseoul.seoulpublicservice.databinding.FragmentCategoryBinding
import com.wannabeinseoul.seoulpublicservice.ui.detail.DetailFragment
//ctrl alt o
class CategoryFragment : Fragment() {

    private lateinit var binding: FragmentCategoryBinding
//    private lateinit var categoryAdapter: CategoryAdapter
    private val viewModel: CategoryViewModel by viewModels{CategoryViewModel.factory}

    private val showDetailFragment = { svcid: String ->
        DetailFragment.newInstance(svcid)
            .show(requireActivity().supportFragmentManager, "Detail")
            }    // 하단에 categoryClick 함수를 만들어서 필요 없을 듯 합니다...

    private val adapter by lazy {
        CategoryAdapter{}
    }

    private var payment: String = ""    // 요금이 무료인지 또는 ""인지
    private var serviceState: List<String> = listOf() // 서비스 상태가 가능인지 아니면 불가능인지
    private var search = "" // 검색어

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.setOnClickListener {
            hideKeyboard()
        }
        initView()
        initViewModel()

        categoryClick()
    }

    private fun initView() {
        binding.reCategory.adapter = adapter

        val category = if (arguments?.getString("category") == "서북병원") "병원" else arguments?.getString("category")
        binding.tvCtTitle.text = "${arguments?.getString("region")} - $category"
        viewModel.updateList(arguments?.getString("region") ?: "", arguments?.getString("category") ?: "")
        //라이브데이터에 리스트를 넣어놈.

        binding.ivCategoryBack.setOnClickListener {
            requireActivity().finish()
        }

        // 무료 버튼 클릭 시
        binding.tvCtFree.setOnClickListener {
            categoryFilter("pay")
        }
        // 예약가능 버튼 클릭 시
        binding.tvCtIsReservationAvailable.setOnClickListener {
            categoryFilter("svc")
        }
        // 검색기능 추가
        binding.etCategorySearch.setOnEditorActionListener { _, actionId, _ ->
            if(actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                hideKeyboard()
                true
            } else {
                false
            }
        }
    }
    private fun performSearch() {
        if(binding.etCategorySearch.text.isNotBlank()) {
            search = binding.etCategorySearch.text.toString()
            viewModel.updateListWithSvcstatnmPay(
                text = search,
                areanm = arguments?.getString("region") ?: "",
                minclassnm = arguments?.getString("category") ?: "",
                pay = payment,
                svcstatnm = serviceState
            )
        } else {
            search = ""
            viewModel.updateList(arguments?.getString("region") ?: "", arguments?.getString("category") ?: "")
        }
    }

    private fun initViewModel() {
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            Log.d("Observe", "잘 되는지 테스트 ${categories.toString().take(255)}")
            binding.tvCategoryEmptyDescription.isVisible = categories.isEmpty()
            adapter.submitList(categories)
        }
    }

    private fun categoryClick() {  // 인터페이스로 받은 svcid를 상세 페이지로 넘기기
        adapter.categoryItemClick = object : CategoryItemClick {
            override fun onClick(svcID: String) {
                val dialog = DetailFragment.newInstance(svcID)
                dialog.show(requireActivity().supportFragmentManager, "CategoryFrag")
            }
        }
    }

    private fun categoryFilter(text: String) {
        when(text) {
            "pay" -> {
                payment = if(payment.isEmpty()) {
                    binding.tvCtFree.setTextColor(Color.parseColor("#F8496C"))
                    "무료"
                } else {
                    binding.tvCtFree.setTextColor(Color.parseColor("#8E8E8E"))
                    ""
                }
            }
            "svc" -> {
                serviceState = if(serviceState.isEmpty()) {
                    binding.tvCtIsReservationAvailable.setTextColor(Color.parseColor("#F8496C"))
                    listOf("접수중","안내중")
                } else {
                    binding.tvCtIsReservationAvailable.setTextColor(Color.parseColor("#8E8E8E"))
                    listOf()
                }
            }
        }
        viewModel.updateListWithSvcstatnmPay(
            text = search,
            areanm = arguments?.getString("region") ?: "",
            minclassnm = arguments?.getString("category") ?: "",
            pay = payment,
            svcstatnm = serviceState
        )
    }

    // 키보드 숨기기
    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etCategorySearch.windowToken, 0)
    }
}