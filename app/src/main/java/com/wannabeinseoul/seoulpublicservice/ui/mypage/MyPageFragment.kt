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
import com.wannabeinseoul.seoulpublicservice.ui.dialog.setting.SettingDialog
import com.wannabeinseoul.seoulpublicservice.ui.main.MainViewModel

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

    private val showSettingDialog = {
        SettingDialog.newInstance()
            .show(requireActivity().supportFragmentManager, "Setting")
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
            )
        )
    }

    private val myPageAdapter by lazy {
        MyPageAdapter(
            lifecycleOwner = viewLifecycleOwner,
            onClearClick = ::basicDialog,
            onReviewedClick = showDetailFragment,
            onSettingClick = showSettingDialog
        )
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
        val curName = app.container.namePrefRepository.load()
        app.userName.value = curName
        mainViewModel.setUserName(curName)
        b.rvMyPage.adapter = myPageAdapter
    }

    private fun initViewModel() = viewModel.let { vm ->
        app.container.savedPrefRepository.savedSvcidListLiveData.observe(viewLifecycleOwner) {
            Log.d(
                JJTAG,
                "옵저버:savedPrefRepository.savedSvcidListLiveData ${it.toString().take(255)}"
            )
            vm.loadSavedList(it)
            myPageAdapter.submitList(fixedItems)
        }

        mainViewModel.userName.observe(viewLifecycleOwner) {
            app.userName.value = it
        }

        mainViewModel.applySynchronization.observe(viewLifecycleOwner) {
            viewModel.loadSavedList(it)
        }
    }

    private fun basicDialog() = AlertDialog.Builder(requireContext()).apply {
        setTitle("저장한 공공서비스 전체 삭제")
        setMessage("정말로 전체 삭제하시겠습니까?")
        setIcon(R.mipmap.ic_launcher)

        setNegativeButton("취소", null)
        setPositiveButton("확인") { _, _ -> viewModel.clearSavedList() }
    }.show()

    override fun onResume() {
        super.onResume()
        Log.d("KDJS", "dd")
    }
}
