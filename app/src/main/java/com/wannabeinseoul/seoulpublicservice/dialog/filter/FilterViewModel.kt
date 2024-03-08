package com.wannabeinseoul.seoulpublicservice.dialog.filter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wannabeinseoul.seoulpublicservice.SeoulPublicServiceApplication
import com.wannabeinseoul.seoulpublicservice.pref.FilterPrefRepository

class FilterViewModel(
    private val filterPrefRepository: FilterPrefRepository
) : ViewModel() {

    private val selectedOptions = listOf(
        mutableListOf<String>(), // 서비스 종류의 '체육시설'
        mutableListOf<String>(), // 서비스 종류의 '교육'
        mutableListOf<String>(), // 서비스 종류의 '문화행사'
        mutableListOf<String>(), // 서비스 종류의 '시설대관'
        mutableListOf<String>(), // 서비스 종류의 '진료'
        mutableListOf<String>(), // 관심 지역의 '서울시'
        mutableListOf<String>(), // 관심 지역의 '이외지역'
        mutableListOf<String>(), // '접수 가능 여부'
        mutableListOf<String>()  // '요금'
    )

    private val _loadedFilterOptions: MutableLiveData<List<List<String>>> = MutableLiveData()
    val loadedFilterOptions: LiveData<List<List<String>>> get() = _loadedFilterOptions

    private val _enableReset: MutableLiveData<Boolean> = MutableLiveData()
    val enableReset: LiveData<Boolean> get() = _enableReset

    fun clearTemporary(index: Int) {
        selectedOptions[index].clear()
    }

    fun saveTemporary(index: Int, selectedOption: String) {
        selectedOptions[index].add(selectedOption)
    }

    fun checkTemporary() {
        _enableReset.value = selectedOptions.any { it.isNotEmpty() }
    }

    fun save() {
        filterPrefRepository.save(selectedOptions)
    }

    fun load() {
        _loadedFilterOptions.value = filterPrefRepository.load()
        _enableReset.value = loadedFilterOptions.value.orEmpty().any { it.isNotEmpty() }
    }

    companion object {
        /** 뷰모델팩토리에서 의존성주입을 해준다 */
        val factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as SeoulPublicServiceApplication)
                val container = application.container
                FilterViewModel(
                    filterPrefRepository = container.filterPrefRepository
                )
            }
        }
    }
}
