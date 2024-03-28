package com.wannabeinseoul.seoulpublicservice.ui.mypage

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.wannabeinseoul.seoulpublicservice.R
import com.wannabeinseoul.seoulpublicservice.SeoulPublicServiceApplication
import com.wannabeinseoul.seoulpublicservice.databinding.FragmentMyPageBinding
import com.wannabeinseoul.seoulpublicservice.ui.detail.DetailFragment
import com.wannabeinseoul.seoulpublicservice.ui.main.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val JJTAG = "jj-마이페이지 프래그먼트"

class MyPageFragment : Fragment() {

    companion object {
        fun newInstance() = MyPageFragment()
    }

    private var _binding: FragmentMyPageBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MyPageViewModel by viewModels { MyPageViewModel.factory }
    private val mainViewModel: MainViewModel by activityViewModels()

    private val app by lazy { requireActivity().application as SeoulPublicServiceApplication }

    private val showDetailFragment = { svcid: String ->
        DetailFragment.newInstance(svcid)
            .show(requireActivity().supportFragmentManager, "Detail")
    }

    private val myPageSavedAdapter by lazy {
        MyPageSavedAdapter { svcid -> showDetailFragment(svcid) }
    }

    private val fixedItems: List<MyPageAdapter.MultiView> by lazy {
        listOf(
            MyPageAdapter.MultiView.Profile(
//                app.userId,
                app.userColor,
                app.userProfileImageDrawable,
                app.userName,
            ) {
                EditProfileDialog.newInstance()
                    .show(requireActivity().supportFragmentManager, "EditProfileDialog")
            },
            MyPageAdapter.MultiView.Saved(
                myPageSavedAdapter,
                viewModel.savedList,
            ),
            MyPageAdapter.MultiView.ReviewedHeader
        )
    }

    private val myPageAdapter by lazy {
        MyPageAdapter(
            lifecycleOwner = viewLifecycleOwner,
            onClearClick = ::basicDialog,
            onReviewedClick = showDetailFragment,
        )
//            .apply {
////                val rows = app.rowList
////                if (rows.isEmpty()) {
////                    var a = 0
////                    submitList(
////                        fixedItems + listOf(
////                            MyPageAdapter.MultiView
////                                .Reviewed(Row.new(svcnm = "${++a} 번째 제목~~", areanm = "가가구")),
////                            MyPageAdapter.MultiView
////                                .Reviewed(Row.new(svcnm = "${++a} 번째 제목~~", areanm = "나나구")),
////                            MyPageAdapter.MultiView
////                                .Reviewed(Row.new(svcnm = "${++a} 번째 제목~~", areanm = "다다구")),
////                            MyPageAdapter.MultiView
////                                .Reviewed(Row.new(svcnm = "${++a} 번째 제목~~", areanm = "가가구")),
////                            MyPageAdapter.MultiView
////                                .Reviewed(Row.new(svcnm = "${++a} 번째 제목~~", areanm = "나나구")),
////                            MyPageAdapter.MultiView
////                                .Reviewed(Row.new(svcnm = "${++a} 번째 제목~~", areanm = "다다구")),
////                            MyPageAdapter.MultiView
////                                .Reviewed(Row.new(svcnm = "${++a} 번째 제목~~", areanm = "가가구")),
////                            MyPageAdapter.MultiView
////                                .Reviewed(Row.new(svcnm = "${++a} 번째 제목~~", areanm = "나나구")),
////                            MyPageAdapter.MultiView
////                                .Reviewed(Row.new(svcnm = "${++a} 번째 제목~~", areanm = "다다구")),
////                        )
////                    )
////                } else {
////                    val random = Random
////                    submitList(fixedItems + List(9) {
////                        MyPageAdapter.MultiView.Reviewed(rows[random.nextInt(rows.size)])
////                    })
////                }
//
//                submitList(fixedItems)
//            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyPageBinding.inflate(inflater, container, false)
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
        b.rvMyPage.adapter = myPageAdapter
    }

    private fun initViewModel() = viewModel.let { vm ->
        app.container.savedPrefRepository.savedSvcidListLiveData.observe(viewLifecycleOwner) {
            Log.d(
                JJTAG,
                "옵저버:savedPrefRepository.savedSvcidListLiveData ${it.toString().take(255)}"
            )
            vm.loadSavedList(it)
        }
        vm.reviewedList.observe(viewLifecycleOwner) { reviewedDataList ->
            Log.d(JJTAG, "옵저버:reviewedList ${reviewedDataList.toString().take(255)}")
            myPageAdapter.submitList(fixedItems +
                    if (reviewedDataList.isEmpty()) listOf(MyPageAdapter.MultiView.ReviewedNothing)
                    else reviewedDataList.map { MyPageAdapter.MultiView.Reviewed(it) }
            )
        }

        mainViewModel.refreshReviewListState.observe(viewLifecycleOwner) {
            CoroutineScope(Dispatchers.IO).launch {
                viewModel.loadReviewedList()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.loadReviewedList()
        }

        // 이거 왜 해놨던 거였지...? 이거 있으니까 로딩 되기 전에 저장한 서비스가 없다고 먼저 떠있음
//        myPageAdapter.setSavedNothingVisible?.invoke(myPageSavedAdapter.itemCount == 0)
    }

    private fun basicDialog() = AlertDialog.Builder(requireContext()).apply {
        setTitle("저장한 공공서비스 전체 삭제")
        setMessage("정말로 전체 삭제하시겠습니까?")
        setIcon(R.mipmap.ic_launcher)

        setNegativeButton("취소", null)
        setPositiveButton("확인") { _, _ -> viewModel.clearSavedList() }
    }.show()

}
