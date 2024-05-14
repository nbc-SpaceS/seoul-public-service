package com.wannabeinseoul.seoulpublicservice.ui.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wannabeinseoul.seoulpublicservice.SeoulPublicServiceApplication
import com.wannabeinseoul.seoulpublicservice.databases.ReservationEntity
import com.wannabeinseoul.seoulpublicservice.usecase.FilterServiceDataOnMapUseCase
import com.wannabeinseoul.seoulpublicservice.usecase.GetSavedServiceUseCase
import com.wannabeinseoul.seoulpublicservice.usecase.LoadSavedFilterOptionsUseCase
import com.wannabeinseoul.seoulpublicservice.usecase.MappingDetailInfoWindowUseCase
import com.wannabeinseoul.seoulpublicservice.usecase.SaveServiceUseCase
import com.wannabeinseoul.seoulpublicservice.usecase.SearchServiceDataOnMapUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.invoke
import kotlinx.coroutines.launch

class MapViewModel(
    private val loadSavedFilterOptionsUseCase: LoadSavedFilterOptionsUseCase,
    private val saveServiceUseCase: SaveServiceUseCase,
    private val getSavedServiceUseCase: GetSavedServiceUseCase,
) : ViewModel() {

    private var _filterCount: Int = 0
    val filterCount: Int get() = _filterCount

    private val _filteringData: MutableLiveData<HashMap<Pair<String, String>, List<DetailInfoWindow>>> =
        MutableLiveData()
    val filteringData: LiveData<HashMap<Pair<String, String>, List<DetailInfoWindow>>> get() = _filteringData

    private val _updateData: MutableLiveData<List<DetailInfoWindow>> = MutableLiveData()
    val updateData: LiveData<List<DetailInfoWindow>> get() = _updateData

    fun setServiceData(mappingData: HashMap<Pair<String, String>, List<DetailInfoWindow>>) {
        val savedOptions = loadSavedOptions()

        _filterCount = savedOptions.count { it.isNotEmpty() }
        _filteringData.value = mappingData
    }

    fun loadSavedOptions(): List<List<String>> = loadSavedFilterOptionsUseCase()

    fun saveService(id: String) {
        saveServiceUseCase(id)
    }

    fun updateInfo(info: List<DetailInfoWindow>) {
        _updateData.value = info
    }

    fun getSavedPrefRepository() = getSavedServiceUseCase()

    companion object {
        /** 뷰모델팩토리에서 의존성주입을 해준다 */
        val factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as SeoulPublicServiceApplication)
                val container = application.container
                MapViewModel(
                    loadSavedFilterOptionsUseCase = container.loadSavedFilterOptionsUseCase,
                    saveServiceUseCase = container.saveServiceUseCase,
                    getSavedServiceUseCase = container.getSavedServiceUseCase,
                )
            }
        }
    }
}