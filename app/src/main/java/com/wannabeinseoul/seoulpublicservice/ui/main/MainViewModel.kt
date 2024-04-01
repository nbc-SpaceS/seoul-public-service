package com.wannabeinseoul.seoulpublicservice.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.wannabeinseoul.seoulpublicservice.ui.dialog.complaint.ComplaintUserInfo
import com.wannabeinseoul.seoulpublicservice.ui.dialog.review.ReviewItem

class MainViewModel: ViewModel() {

    private var _selectedServiceId: String = ""
    val selectedServiceId: String get() = _selectedServiceId

    private var _currentReviewList: List<ReviewItem> = listOf()
    val currentReviewList: List<ReviewItem> get() = _currentReviewList

    private var _complaintUserInfo: ComplaintUserInfo = ComplaintUserInfo.newData()
    val complaintUserInfo: ComplaintUserInfo get() = _complaintUserInfo

    private val _applyFilter: MutableLiveData<Boolean> = MutableLiveData()
    val applyFilter: LiveData<Boolean> get() = _applyFilter

    private val _refreshReviewListState: MutableLiveData<Boolean> = MutableLiveData()
    val refreshReviewListState: LiveData<Boolean> get() = _refreshReviewListState

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

//    fun getServiceId() = selectedServiceId

    fun setReviewListState(flag: Boolean) {
        _refreshReviewListState.value = flag
    }

    fun setCurrentReviewList(list : List<ReviewItem>) {
        _currentReviewList = list
    }

//    fun getCurrentReviewList(): List<ReviewItem> = currentReviewList

    fun setComplaintUserInfo(userInfo: ComplaintUserInfo) {
        _complaintUserInfo = userInfo.copy(svcId = selectedServiceId)
    }

    fun setRegion(region: String) {
        _selectRegion.value = region
    }

    fun moveSelectRegions(flag: Boolean) {
        _moveSelectRegions.value = flag
    }
}