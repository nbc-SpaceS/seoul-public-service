package com.wannabeinseoul.seoulpublicservice.ui.dialog.filter

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.google.android.material.chip.Chip
import com.wannabeinseoul.seoulpublicservice.R
import com.wannabeinseoul.seoulpublicservice.databinding.FragmentFilterBinding
import com.wannabeinseoul.seoulpublicservice.ui.main.MainViewModel

class FilterFragment : DialogFragment() {

    companion object {
        fun newInstance() = FilterFragment()
    }

    private var _binding: FragmentFilterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FilterViewModel by viewModels { FilterViewModel.factory }
    private val mainViewModel: MainViewModel by activityViewModels { MainViewModel.factory }

    private val filterOptions by lazy {
        listOf(
            listOf(
                R.id.chip_1_1_1 to "축구장",
                R.id.chip_1_1_2 to "테니스장",
                R.id.chip_1_1_3 to "탁구장",
                R.id.chip_1_1_4 to "골프장",
                R.id.chip_1_1_5 to "야구장",
                R.id.chip_1_1_6 to "배구장",
                R.id.chip_1_1_7 to "족구장",
                R.id.chip_1_1_8 to "풋살장",
                R.id.chip_1_1_9 to "배드민턴장",
                R.id.chip_1_1_10 to "다목적경기장",
                R.id.chip_1_1_11 to "체육관",
                R.id.chip_1_1_12 to "농구장",
            ),
            listOf(
                R.id.chip_1_2_1 to "교양/어학",
                R.id.chip_1_2_2 to "정보통신",
                R.id.chip_1_2_3 to "역사",
                R.id.chip_1_2_4 to "자연과학",
                R.id.chip_1_2_5 to "도시농업",
                R.id.chip_1_2_6 to "청년정보",
                R.id.chip_1_2_7 to "스포츠",
                R.id.chip_1_2_8 to "미술제작",
                R.id.chip_1_2_9 to "공예/취미",
                R.id.chip_1_2_10 to "전문/자격증",
                R.id.chip_1_2_11 to "기타",
            ),
            listOf(
                R.id.chip_1_3_1 to "전시/관람",
                R.id.chip_1_3_2 to "교육체험",
                R.id.chip_1_3_3 to "문화행사",
                R.id.chip_1_3_4 to "산림여가",
                R.id.chip_1_3_5 to "공원탐방",
                R.id.chip_1_3_6 to "서울형키즈카페",
                R.id.chip_1_3_7 to "농장체험",
            ),
            listOf(
                R.id.chip_1_4_1 to "다목적실",
                R.id.chip_1_4_2 to "공연장",
                R.id.chip_1_4_3 to "강당",
                R.id.chip_1_4_4 to "주민공유공간",
                R.id.chip_1_4_5 to "캠핑장",
                R.id.chip_1_4_6 to "청년공간",
                R.id.chip_1_4_7 to "녹화장소",
                R.id.chip_1_4_8 to "회의실",
                R.id.chip_1_4_9 to "강의실",
                R.id.chip_1_4_10 to "민원/기타",
            ),
            listOf(
                R.id.chip_1_5_1 to "병원",
                R.id.chip_1_5_2 to "어린이병원",
                R.id.chip_1_5_3 to "장애인버스",
            ),
            listOf(
                R.id.chip_2_1_1 to "강남구",
                R.id.chip_2_1_2 to "강동구",
                R.id.chip_2_1_3 to "강북구",
                R.id.chip_2_1_4 to "강서구",
                R.id.chip_2_1_5 to "관악구",
                R.id.chip_2_1_6 to "광진구",
                R.id.chip_2_1_7 to "구로구",
                R.id.chip_2_1_8 to "금천구",
                R.id.chip_2_1_9 to "노원구",
                R.id.chip_2_1_10 to "도봉구",
                R.id.chip_2_1_11 to "동대문구",
                R.id.chip_2_1_12 to "동작구",
                R.id.chip_2_1_13 to "마포구",
                R.id.chip_2_1_14 to "서대문구",
                R.id.chip_2_1_15 to "서초구",
                R.id.chip_2_1_16 to "성동구",
                R.id.chip_2_1_17 to "성북구",
                R.id.chip_2_1_18 to "송파구",
                R.id.chip_2_1_19 to "양천구",
                R.id.chip_2_1_20 to "영등포구",
                R.id.chip_2_1_21 to "용산구",
                R.id.chip_2_1_22 to "은평구",
                R.id.chip_2_1_23 to "종로구",
                R.id.chip_2_1_24 to "중구",
                R.id.chip_2_1_25 to "중랑구",
            ),
            listOf(
                R.id.chip_2_2_1 to "서울제외지역"
            ),
            listOf(
                R.id.chip_3_1_1 to "접수중",
                R.id.chip_3_1_2 to "안내중",
            ),
            listOf(
                R.id.chip_4_1_1 to "무료",
                R.id.chip_4_1_2 to "유료",
                R.id.chip_4_1_3 to "유료(요금안내문의)",
            )
        )
    }

    private val headerList by lazy {
        listOf(
            binding.tvFilterTitle1Header1,
            binding.tvFilterTitle1Header2,
            binding.tvFilterTitle1Header3,
            binding.tvFilterTitle1Header4,
            binding.tvFilterTitle1Header5,
            binding.tvFilterTitle2Header1,
            binding.tvFilterTitle2Header2
        )
    }

    private val chipGroupList by lazy {
        listOf(
            binding.cgFilterTitle1Header1,
            binding.cgFilterTitle1Header2,
            binding.cgFilterTitle1Header3,
            binding.cgFilterTitle1Header4,
            binding.cgFilterTitle1Header5,
            binding.cgFilterTitle2Header1,
            binding.cgFilterTitle2Header2,
            binding.cgFilterTitle3Header1,
            binding.cgFilterTitle4Header1
        )
    }

    private val chipGroupCountList by lazy {
        listOf(
            binding.tvFilterTitle1Header1Count,
            binding.tvFilterTitle1Header2Count,
            binding.tvFilterTitle1Header3Count,
            binding.tvFilterTitle1Header4Count,
            binding.tvFilterTitle1Header5Count,
            binding.tvFilterTitle2Header1Count,
            binding.tvFilterTitle2Header2Count,
        )
    }

    private val moreButtonList by lazy {
        listOf(
            binding.ivFilterTitle1Header1Btn,
            binding.ivFilterTitle1Header2Btn,
            binding.ivFilterTitle1Header3Btn,
            binding.ivFilterTitle1Header4Btn,
            binding.ivFilterTitle1Header5Btn,
            binding.ivFilterTitle2Header1Btn,
            binding.ivFilterTitle2Header2Btn
        )
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext(), R.style.DetailTransparent)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
        initViewModel()
    }

    private fun initView() = with(binding) {

        viewModel.load()

        ivFilterBackBtn.setOnClickListener {
            dismiss()
        }

        tvFilterResetBtn.setOnClickListener {
            chipGroupList.forEachIndexed { index, chipGroup ->
                viewModel.clearTemporary(index)
                chipGroup.clearCheck()
            }
            Toast.makeText(requireContext(), "필터가 초기화되었습니다.", Toast.LENGTH_SHORT).show()
            tvFilterResetBtn.isEnabled = false
        }

        btnFilterApply.setOnClickListener {
            viewModel.save()
            mainViewModel.setFilterState(true)
            dismiss()
        }

        headerList.forEachIndexed { index, header ->
            header.setOnClickListener {
                if (chipGroupList[index].isVisible) {
                    moreButtonList[index].setImageResource(R.drawable.ic_more)
                    val slideUp = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up)
                    slideUp.setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationStart(p0: Animation?) = Unit
                        override fun onAnimationRepeat(p0: Animation?) = Unit
                        override fun onAnimationEnd(p0: Animation?) {
                            chipGroupList[index].visibility = View.GONE
                        }
                    })
                    chipGroupList[index].startAnimation(slideUp)
//                    chipGroupList[index].visibility = View.GONE
                } else {
                    moreButtonList[index].setImageResource(R.drawable.ic_less)
                    val slideDown = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_down)
                    chipGroupList[index].startAnimation(slideDown)
                    chipGroupList[index].visibility = View.VISIBLE
                }
            }
        }

        chipGroupList.forEachIndexed { index, chipGroup ->
            chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
                viewModel.clearTemporary(index)

                for (id in checkedIds) {
                    val selectedOption = filterOptions[index].first { it.first == id }.second
                    viewModel.saveTemporary(index, selectedOption)
                    viewModel.checkTemporary()
                }

                if (index < 7) {
                    if (checkedIds.size != 0) {
                        chipGroupCountList[index].text = "${checkedIds.size}개 선택"
                        chipGroupCountList[index].isVisible = true
                    } else {
                        chipGroupCountList[index].isVisible = false
                    }
                }
            }
        }
    }

    private fun initViewModel() = with(viewModel) {
        loadedFilterOptions.observe(viewLifecycleOwner) { filter ->
            filter.forEachIndexed { index, options ->
                viewModel.clearTemporary(index)
                options.forEach { option ->
                    val loadedChip = chipGroupList[index].findViewById(filterOptions[index].first { it.second == option}.first) as Chip
                    loadedChip.isChecked = true
                }
            }
        }

        enableReset.observe(viewLifecycleOwner) {
            binding.tvFilterResetBtn.isEnabled = it
        }
    }
}