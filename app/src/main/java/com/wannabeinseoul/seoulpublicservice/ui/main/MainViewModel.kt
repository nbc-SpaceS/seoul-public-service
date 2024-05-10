package com.wannabeinseoul.seoulpublicservice.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wannabeinseoul.seoulpublicservice.SeoulPublicServiceApplication
import com.wannabeinseoul.seoulpublicservice.databases.ReservationEntity
import com.wannabeinseoul.seoulpublicservice.ui.map.DetailInfoWindow
import com.wannabeinseoul.seoulpublicservice.ui.map.MapViewModel
import com.wannabeinseoul.seoulpublicservice.usecase.FilterServiceDataOnMapUseCase
import com.wannabeinseoul.seoulpublicservice.usecase.GetSavedServiceUseCase
import com.wannabeinseoul.seoulpublicservice.usecase.LoadSavedFilterOptionsUseCase
import com.wannabeinseoul.seoulpublicservice.usecase.MappingDetailInfoWindowUseCase
import com.wannabeinseoul.seoulpublicservice.usecase.SearchServiceDataOnMapUseCase

class MainViewModel(
    private val loadSavedFilterOptionsUseCase: LoadSavedFilterOptionsUseCase,
    private val filterServiceDataOnMapUseCase: FilterServiceDataOnMapUseCase,
    private val searchServiceDataOnMapUseCase: SearchServiceDataOnMapUseCase,
    private val mappingDetailInfoWindowUseCase: MappingDetailInfoWindowUseCase,
    private val getSavedServiceUseCase: GetSavedServiceUseCase
): ViewModel() {

    private var _selectedServiceId: String = ""
    val selectedServiceId: String get() = _selectedServiceId

    private var _prevSavedServices: List<String> = listOf()
    val prevSavedServices: List<String> get() = _prevSavedServices

    private var _userName: MutableLiveData<String> = MutableLiveData()
    val userName: LiveData<String> get() = _userName

    private val _applyFilter: MutableLiveData<Boolean> = MutableLiveData()
    val applyFilter: LiveData<Boolean> get() = _applyFilter

    private val _selectRegion: MutableLiveData<String> = MutableLiveData()
    val selectRegion: LiveData<String> get() = _selectRegion

    private val _moveSelectRegions: MutableLiveData<Boolean> = MutableLiveData()
    val moveSelectRegions: LiveData<Boolean> get() = _moveSelectRegions

    private val _applySynchronization: MutableLiveData<List<String>> = MutableLiveData()
    val applySynchronization: LiveData<List<String>> get() = _applySynchronization

    private val _mappingData: MutableLiveData<HashMap<Pair<String, String>, List<DetailInfoWindow>>> =
        MutableLiveData()
    val mappingData: LiveData<HashMap<Pair<String, String>, List<DetailInfoWindow>>> get() = _mappingData

    fun setFilterState(flag: Boolean) {
        _applyFilter.value = flag
    }

    fun setServiceId(id: String) {
        _selectedServiceId = id
    }

    fun setRegion(region: String) {
        _selectRegion.value = region
    }

    fun moveSelectRegions(flag: Boolean) {
        _moveSelectRegions.value = flag
    }

    fun setUserName(name: String) {
        _userName.postValue(name)
    }

    fun synchronizeData(name: String, list: List<String>) {
        _userName.value = name
        _applySynchronization.value = list
    }

    fun setMappingData(isSearch: Boolean = false) {
        val savedOptions = loadSavedFilterOptionsUseCase()
        val list = getSavedServiceUseCase().getSvcidList()

        if (isSearch || _prevSavedServices != list) {
            val filteringData = filterServiceDataOnMapUseCase(savedOptions)
            val mappingData: HashMap<Pair<String, String>, List<DetailInfoWindow>> = hashMapOf()

            filteringData.forEach {
                mappingData[it.key] = mappingDetailInfoWindowUseCase(it.value)
            }

            _mappingData.value = mappingData
        }

        _prevSavedServices = list
    }


    fun setMappingData(word: String, isSearch: Boolean = false) {
        val savedOptions = loadSavedFilterOptionsUseCase()
        val list = getSavedServiceUseCase().getSvcidList()

        if (isSearch || _prevSavedServices != list) {
            val filteringData = searchServiceDataOnMapUseCase(word, savedOptions)
            val mappingData: HashMap<Pair<String, String>, List<DetailInfoWindow>> = hashMapOf()

            filteringData.forEach {
                mappingData[it.key] = mappingDetailInfoWindowUseCase(it.value)
            }

            _mappingData.value = mappingData
        }

        _prevSavedServices = list
    }

    companion object {
        val factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SeoulPublicServiceApplication)
                val container = application.container
                MainViewModel(
                    loadSavedFilterOptionsUseCase = container.loadSavedFilterOptionsUseCase,
                    filterServiceDataOnMapUseCase = container.filterServiceDataOnMapUseCase,
                    getSavedServiceUseCase = container.getSavedServiceUseCase,
                    mappingDetailInfoWindowUseCase = container.mappingDetailInfoWindowUseCase,
                    searchServiceDataOnMapUseCase = container.searchServiceDataOnMapUseCase
                )
            }
        }
    }
}