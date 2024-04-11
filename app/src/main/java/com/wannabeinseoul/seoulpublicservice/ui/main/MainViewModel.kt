package com.wannabeinseoul.seoulpublicservice.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel: ViewModel() {

    private var _selectedServiceId: String = ""
    val selectedServiceId: String get() = _selectedServiceId

    private var _userName: String = ""
    val userName: String get() = _userName

    private val _applyFilter: MutableLiveData<Boolean> = MutableLiveData()
    val applyFilter: LiveData<Boolean> get() = _applyFilter

    private val _selectRegion: MutableLiveData<String> = MutableLiveData()
    val selectRegion: LiveData<String> get() = _selectRegion

    private val _moveSelectRegions: MutableLiveData<Boolean> = MutableLiveData()
    val moveSelectRegions: LiveData<Boolean> get() = _moveSelectRegions

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
        _userName = name
    }
}