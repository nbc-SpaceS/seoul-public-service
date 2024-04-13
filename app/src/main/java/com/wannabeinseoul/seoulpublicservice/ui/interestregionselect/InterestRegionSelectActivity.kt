package com.wannabeinseoul.seoulpublicservice.ui.interestregionselect

import android.graphics.Paint
import android.os.Bundle
import android.widget.CompoundButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.wannabeinseoul.seoulpublicservice.databinding.ActivityInterestRegionSelectBinding


class InterestRegionSelectActivity : AppCompatActivity() {

    private val binding: ActivityInterestRegionSelectBinding by lazy {
        ActivityInterestRegionSelectBinding.inflate(layoutInflater)
    }

    private val viewModel: InterestRegionSelectViewModel by viewModels { InterestRegionSelectViewModel.factory }

    private val matchingRegion by lazy {
        hashMapOf(
            binding.cbInterestRegionSelectBtn1 to "강남구",
            binding.cbInterestRegionSelectBtn2 to "강동구",
            binding.cbInterestRegionSelectBtn3 to "강북구",
            binding.cbInterestRegionSelectBtn4 to "강서구",
            binding.cbInterestRegionSelectBtn5 to "관악구",
            binding.cbInterestRegionSelectBtn6 to "광진구",
            binding.cbInterestRegionSelectBtn7 to "구로구",
            binding.cbInterestRegionSelectBtn8 to "금천구",
            binding.cbInterestRegionSelectBtn9 to "노원구",
            binding.cbInterestRegionSelectBtn10 to "도봉구",
            binding.cbInterestRegionSelectBtn11 to "동대문구",
            binding.cbInterestRegionSelectBtn12 to "동작구",
            binding.cbInterestRegionSelectBtn13 to "마포구",
            binding.cbInterestRegionSelectBtn14 to "서대문구",
            binding.cbInterestRegionSelectBtn15 to "서초구",
            binding.cbInterestRegionSelectBtn16 to "성동구",
            binding.cbInterestRegionSelectBtn17 to "성북구",
            binding.cbInterestRegionSelectBtn18 to "송파구",
            binding.cbInterestRegionSelectBtn19 to "양천구",
            binding.cbInterestRegionSelectBtn20 to "영등포구",
            binding.cbInterestRegionSelectBtn21 to "용산구",
            binding.cbInterestRegionSelectBtn22 to "은평구",
            binding.cbInterestRegionSelectBtn23 to "종로구",
            binding.cbInterestRegionSelectBtn24 to "중구",
            binding.cbInterestRegionSelectBtn25 to "중랑구"
        )
    }

    private val listener = CompoundButton.OnCheckedChangeListener { view, isChecked ->
        val region = matchingRegion[view] ?: ""
        if (isChecked) {
            if (viewModel.getListSize() == 3) {
                view.isChecked = false
                Toast.makeText(this@InterestRegionSelectActivity, "최대선택개수초과", Toast.LENGTH_SHORT)
                    .show()
            } else {
                viewModel.selectCheckbox(region)
            }
        } else {
            viewModel.unselectCheckbox(region)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initView()
        initViewModel()
    }

    private fun initView() = with(binding) {
        viewModel.loadRegion()

        tvInterestResetRegions.paintFlags = Paint.UNDERLINE_TEXT_FLAG
        tvInterestResetRegions.isVisible = viewModel.checkList()

        ivInterestRegionSelectBackBtn.setOnClickListener {

            finish()
        }

        btnInterestRegionSelectOkay.setOnClickListener {
            viewModel.saveRegion()
            setResult(RESULT_OK)
            finish()
        }

        tvInterestResetRegions.setOnClickListener {
            viewModel.resetRegions()
            viewModel.saveRegion()
            setResult(RESULT_OK)
            finish()
        }

        matchingRegion.keys.forEach {
            it.setOnCheckedChangeListener(listener)
        }
    }

    private fun initViewModel() = with(viewModel) {
        enableButton.observe(this@InterestRegionSelectActivity) {
            binding.btnInterestRegionSelectOkay.isEnabled = it
        }

        loadRegionList.observe(this@InterestRegionSelectActivity) { list ->
            list.forEach { region ->
                matchingRegion.keys.first { matchingRegion[it] == region }.isChecked = true
            }
        }
    }
}