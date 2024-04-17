package com.wannabeinseoul.seoulpublicservice.ui.mypage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wannabeinseoul.seoulpublicservice.SeoulPublicServiceApplication
import com.wannabeinseoul.seoulpublicservice.db_by_memory.DbMemoryRepository
import com.wannabeinseoul.seoulpublicservice.pref.SavedPrefRepository
import com.wannabeinseoul.seoulpublicservice.ui.recommendation.RecommendationData
import com.wannabeinseoul.seoulpublicservice.ui.recommendation.convertToRecommendationData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyPageViewModel(
    private val savedPrefRepository: SavedPrefRepository,
    private val dbMemoryRepository: DbMemoryRepository,
) : ViewModel() {

    private var _savedList: MutableLiveData<List<RecommendationData?>> = MutableLiveData()
    val savedList: LiveData<List<RecommendationData?>> get() = _savedList

    fun loadSavedList(svcidList: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            _savedList.postValue(
                svcidList.mapIndexedNotNull { _, svcId ->
                    dbMemoryRepository.findBySvcid(svcId)?.convertToRecommendationData()
                        .also { it?.reviewCount = 0 }
                }
            )
        }
    }

    fun clearSavedList() {
        savedPrefRepository.clear()
    }

    companion object {
        val factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as SeoulPublicServiceApplication).container
                MyPageViewModel(
                    savedPrefRepository = container.savedPrefRepository,
                    dbMemoryRepository = container.dbMemoryRepository,
                )
            }
        }
    }

}
