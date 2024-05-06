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
    private val filterServiceDataOnMapUseCase: FilterServiceDataOnMapUseCase,
    private val saveServiceUseCase: SaveServiceUseCase,
    private val mappingDetailInfoWindowUseCase: MappingDetailInfoWindowUseCase,
    private val getSavedServiceUseCase: GetSavedServiceUseCase,
    private val searchServiceDataOnMapUseCase: SearchServiceDataOnMapUseCase
) : ViewModel() {

    private var readyMap: Boolean = false
    private var readyData: Boolean = false
    private var searchWord = ""

    private var _filterCount: Int = 0
    val filterCount: Int get() = _filterCount

    private val _canStart: MutableLiveData<Boolean> = MutableLiveData()
    val canStart: LiveData<Boolean> get() = _canStart

    private val _filteringData: MutableLiveData<HashMap<Pair<String, String>, List<ReservationEntity>>> =
        MutableLiveData()
    val filteringData: LiveData<HashMap<Pair<String, String>, List<ReservationEntity>>> get() = _filteringData

    private val _updateData: MutableLiveData<List<DetailInfoWindow>> = MutableLiveData()
    val updateData: LiveData<List<DetailInfoWindow>> get() = _updateData

    fun setServiceData() {
        _canStart.value = false
        readyData = false
        val savedOptions = loadSavedOptions()

        _filterCount = savedOptions.count { it.isNotEmpty() }

        viewModelScope.launch(Dispatchers.IO) {
            _filteringData.postValue(filterServiceDataOnMapUseCase(savedOptions))

            readyData = true
            if (readyMap) {
                _canStart.postValue(true)
            }
        }
    }

    fun setServiceData(word: String) {
        _canStart.value = false
        readyData = false
        val savedOptions = loadSavedOptions()

        _filterCount = savedOptions.count { it.isNotEmpty() }

        viewModelScope.launch(Dispatchers.IO) {
            _filteringData.postValue(searchServiceDataOnMapUseCase(word, savedOptions))

            readyData = true
            if (readyMap) {
                _canStart.postValue(true)
            }
        }
    }

    fun updateServiceData(word: String) {
        val savedOptions = loadSavedOptions()
        searchWord = word

        _filterCount = savedOptions.count { it.isNotEmpty() }

        viewModelScope.launch(Dispatchers.IO) {
            _filteringData.postValue(searchServiceDataOnMapUseCase(searchWord, savedOptions))

            _canStart.postValue(true)
        }
    }

    fun loadSavedOptions(): List<List<String>> = loadSavedFilterOptionsUseCase()

    fun checkReadyMap() {
        readyMap = true
        if (readyData) {
            _canStart.postValue(true)
        }
    }

    fun saveService(id: String) {
        saveServiceUseCase(id)
    }

    fun updateInfo(info: List<ReservationEntity>) {
        _updateData.value = mappingDetailInfoWindowUseCase(info)
    }

    fun initMap() {
        readyMap = false
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
                    filterServiceDataOnMapUseCase = container.filterServiceDataOnMapUseCase,
                    saveServiceUseCase = container.saveServiceUseCase,
                    mappingDetailInfoWindowUseCase = container.mappingDetailInfoWindowUseCase,
                    getSavedServiceUseCase = container.getSavedServiceUseCase,
                    searchServiceDataOnMapUseCase = container.searchServiceDataOnMapUseCase
                )
            }
        }
    }
}