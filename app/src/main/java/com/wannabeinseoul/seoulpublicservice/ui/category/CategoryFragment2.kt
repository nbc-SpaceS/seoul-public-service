package com.wannabeinseoul.seoulpublicservice.ui.category

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wannabeinseoul.seoulpublicservice.R
import com.wannabeinseoul.seoulpublicservice.databinding.FragmentCategoryBinding
import com.wannabeinseoul.seoulpublicservice.ui.detail.DetailCloseInterface
import com.wannabeinseoul.seoulpublicservice.ui.detail.DetailFragment
import com.wannabeinseoul.seoulpublicservice.ui.main.MainViewModel

private const val CATEGORY_PARAM1 = "category_param1"
private const val CATEGORY_PARAM2 = "category_param2"

//ctrl alt o
class CategoryFragment2 : DialogFragment() {

    private var _binding: FragmentCategoryBinding? = null
    private val binding get()= _binding!!

    private val viewModel: CategoryViewModel by viewModels { CategoryViewModel.factory }
    private val mainViewModel: MainViewModel by activityViewModels { MainViewModel.factory }

    private val adapter by lazy {
        CategoryAdapter {}
    }

    private var payment: String = ""    // 요금이 무료인지 또는 ""인지
    private var serviceState: List<String> = listOf() // 서비스 상태가 가능인지 아니면 불가능인지
    private var search = "" // 검색어

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)

            if (!recyclerView.canScrollVertically(-1)) {
                // 리스트가 최상단에 도달했을 때
                binding.fabRecentFloating.visibility = View.GONE
            } else if (!recyclerView.canScrollVertically(1)) {
                // 리스트가 최하단에 도달했을 때
                binding.fabRecentFloating.visibility = View.VISIBLE
            } else {
                // 리스트가 중간에 있을 때
                binding.fabRecentFloating.visibility = View.VISIBLE
            }
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) =
        Dialog(requireContext(), R.style.DetailTransparent)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.setOnClickListener {
            hideKeyboard()
        }
        initView()
        initViewModel()

        categoryClick()

        binding.reCategory.addOnScrollListener(scrollListener)

        // FloatingActionButton 클릭 이벤트 처리
        binding.fabRecentFloating.setOnClickListener {
            binding.reCategory.smoothScrollToPosition(0)
        }
    }

    private fun initView() {
        binding.reCategory.adapter = adapter

        val category =
            if (arguments?.getString(CATEGORY_PARAM1) == "서북병원") "병원" else arguments?.getString(CATEGORY_PARAM1)
        val region = arguments?.getString(CATEGORY_PARAM2)

        binding.tvCtTitle.text = if (region == "") "전체 - $category" else "$region - $category"

        //라이브데이터에 리스트를 넣어놈.
        viewModel.updateList(region ?: "", category ?: "")

        binding.ivCategoryBack.setOnClickListener {
            dismiss()
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
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                hideKeyboard()
                true
            } else {
                false
            }
        }
    }

    private fun performSearch() {
        val category =
            if (arguments?.getString(CATEGORY_PARAM1) == "서북병원") "병원" else arguments?.getString(CATEGORY_PARAM1)
        val region = arguments?.getString(CATEGORY_PARAM2)

        if (binding.etCategorySearch.text.isNotBlank()) {
            search = binding.etCategorySearch.text.toString()
            viewModel.updateListWithSvcstatnmPay(
                text = search,
                areanm = region ?: "",
                minclassnm = category ?: "",
                pay = payment,
                svcstatnm = serviceState
            )
        } else {
            search = ""
            viewModel.updateList(
                region ?: "", category ?: ""
            )
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
                dialog.setCloseListener(object : DetailCloseInterface {
                    override fun onDialogClosed() {
                        mainViewModel.setMappingData()
                    }
                })
                dialog.show(requireActivity().supportFragmentManager, "CategoryFrag")
            }
        }
    }

    private fun categoryFilter(text: String) {
        when (text) {
            "pay" -> {
                payment = if (payment.isEmpty()) {
                    binding.tvCtFree.setTextColor(Color.parseColor("#F8496C"))
                    "무료"
                } else {
                    binding.tvCtFree.setTextColor(Color.parseColor("#8E8E8E"))
                    ""
                }
            }

            "svc" -> {
                serviceState = if (serviceState.isEmpty()) {
                    binding.tvCtIsReservationAvailable.setTextColor(Color.parseColor("#F8496C"))
                    listOf("접수중", "안내중")
                } else {
                    binding.tvCtIsReservationAvailable.setTextColor(Color.parseColor("#8E8E8E"))
                    listOf()
                }
            }
        }

        viewModel.updateListWithSvcstatnmPay(
            text = search,
            areanm = arguments?.getString(CATEGORY_PARAM2) ?: "",
            minclassnm = arguments?.getString(CATEGORY_PARAM1) ?: "",
            pay = payment,
            svcstatnm = serviceState
        )
    }

    // 키보드 숨기기
    private fun hideKeyboard() {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etCategorySearch.windowToken, 0)
    }

    companion object {
        @JvmStatic
        fun newInstance(category: String, region: String) =
            CategoryFragment2().apply {
                arguments = Bundle().apply {
                    putString(CATEGORY_PARAM1, category)
                    putString(CATEGORY_PARAM2, region)
                }
            }
    }
}