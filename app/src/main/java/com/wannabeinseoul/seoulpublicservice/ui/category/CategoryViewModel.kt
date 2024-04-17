package com.wannabeinseoul.seoulpublicservice.ui.category

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wannabeinseoul.seoulpublicservice.SeoulPublicServiceApplication
import com.wannabeinseoul.seoulpublicservice.databases.ReservationRepository
import com.wannabeinseoul.seoulpublicservice.db_by_memory.DbMemoryRepository
import com.wannabeinseoul.seoulpublicservice.pref.CategoryPrefRepository
import com.wannabeinseoul.seoulpublicservice.pref.RegionPrefRepository
import com.wannabeinseoul.seoulpublicservice.seoul.SeoulPublicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CategoryViewModel(
    private val categoryPrefRepository: CategoryPrefRepository,
    private val regionPrefRepository: RegionPrefRepository,
    private val seoulPublicRepository: SeoulPublicRepository,
    private val dbMemoryRepository: DbMemoryRepository,
    private val reservationRepository: ReservationRepository
) : ViewModel() {

    private val _categories = MutableLiveData<List<CategoryData>>(emptyList())
    val categories: LiveData<List<CategoryData>> get() = _categories

    fun updateList (areanm: String, minclassnm: String) {
        val filteredList = if (areanm == "") {
            dbMemoryRepository.getFiltered(minclassnm = listOf(minclassnm)).convertToCategoryDataList()
        } else {
            dbMemoryRepository.getFiltered(areanm = listOf(areanm), minclassnm = listOf(minclassnm)).convertToCategoryDataList()
        }
        _categories.value = filteredList
        //minclassnm은 소분류명
    }

    fun updateListWithSvcstatnmPay (text: String, areanm: String, minclassnm: String, pay: String, svcstatnm: List<String>) {
        viewModelScope.launch {
            val filteredList = withContext(Dispatchers.IO) {
                reservationRepository.searchFilter(text = text, typeSub = listOf(minclassnm), typeLoc = listOf(areanm), typePay = listOf(pay), typeSvc = svcstatnm).convertToCategoryDataList()
            }
            _categories.value = filteredList
        }
    }

    companion object {
        val factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as SeoulPublicServiceApplication).container
                CategoryViewModel(
                    categoryPrefRepository = container.categoryPrefRepository,
                    regionPrefRepository = container.regionPrefRepository,
                    seoulPublicRepository = container.seoulPublicRepository,
                    dbMemoryRepository = container.dbMemoryRepository,
                    reservationRepository = container.reservationRepository
                )
            }
        }
    }
}