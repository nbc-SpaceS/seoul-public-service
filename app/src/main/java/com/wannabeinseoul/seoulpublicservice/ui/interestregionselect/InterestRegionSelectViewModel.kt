package com.wannabeinseoul.seoulpublicservice.ui.interestregionselect

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wannabeinseoul.seoulpublicservice.SeoulPublicServiceApplication
import com.wannabeinseoul.seoulpublicservice.pref.RegionPrefRepository

class InterestRegionSelectViewModel(
    private val regionPrefRepository: RegionPrefRepository
) : ViewModel() {

    private val _loadRegionList: MutableLiveData<List<String>> = MutableLiveData()
    val loadRegionList: LiveData<List<String>> get() = _loadRegionList

    private val _selectedRegionList: MutableLiveData<List<String>> = MutableLiveData()
    val selectedRegionList: LiveData<List<String>> get() = _selectedRegionList

    private val _enableButton: MutableLiveData<Boolean> = MutableLiveData()
    val enableButton: LiveData<Boolean> get() = _enableButton

    fun selectCheckbox(num: String) {
        _selectedRegionList.value = selectedRegionList.value.orEmpty().toMutableList().apply {
            add(num)
            // sort 기능 추가
        }
        enableOkayButton()
    }

    fun unselectCheckbox(num: String) {
        _selectedRegionList.value = selectedRegionList.value.orEmpty().toMutableList().apply {
            remove(num)
        }
        enableOkayButton()
    }

    private fun enableOkayButton() {
        _enableButton.value = selectedRegionList.value?.isNotEmpty()
    }

    fun resetRegions() {
        _selectedRegionList.value = emptyList()
    }

    fun saveRegion() {
        regionPrefRepository.save(selectedRegionList.value.orEmpty().toMutableList())
    }

    fun loadRegion() {
        _loadRegionList.value = selectedRegionList.value.orEmpty().toMutableList().apply {
            addAll(regionPrefRepository.load())
        }
    }

    fun getListSize(): Int = selectedRegionList.value.orEmpty().size

    fun checkList(): Boolean =
        selectedRegionList.value.orEmpty().isNotEmpty() || loadRegionList.value.orEmpty().isNotEmpty()

    companion object {
        /** 뷰모델팩토리에서 의존성주입을 해준다 */
        val factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as SeoulPublicServiceApplication)
                val container = application.container
                InterestRegionSelectViewModel(
                    regionPrefRepository = container.regionPrefRepository
                )
            }
        }
    }
}