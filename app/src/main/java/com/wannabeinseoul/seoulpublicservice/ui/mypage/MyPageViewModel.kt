package com.wannabeinseoul.seoulpublicservice.ui.mypage

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wannabeinseoul.seoulpublicservice.SeoulPublicServiceApplication
import com.wannabeinseoul.seoulpublicservice.databases.firebase.ReviewRepository
import com.wannabeinseoul.seoulpublicservice.databases.firebase.UserRepository
import com.wannabeinseoul.seoulpublicservice.db_by_memory.DbMemoryRepository
import com.wannabeinseoul.seoulpublicservice.pref.IdPrefRepository
import com.wannabeinseoul.seoulpublicservice.pref.SavedPrefRepository
import com.wannabeinseoul.seoulpublicservice.seoul.Row
import com.wannabeinseoul.seoulpublicservice.usecase.GetDetailSeoulUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyPageViewModel(
    private val savedPrefRepository: SavedPrefRepository,
    private val getDetailSeoulUseCase: GetDetailSeoulUseCase,
    private val dbMemoryRepository: DbMemoryRepository,
    private val idPrefRepository: IdPrefRepository,
    private val userRepository: UserRepository,
    private val reviewRepository: ReviewRepository,
) : ViewModel() {

    private var _savedList: MutableLiveData<List<Row?>> = MutableLiveData(emptyList())
    val savedList: LiveData<List<Row?>> get() = _savedList

    private var _reviewedList: MutableLiveData<List<ReviewedData>> = MutableLiveData(emptyList())
    val reviewedList: LiveData<List<ReviewedData>> get() = _reviewedList

    init {
        viewModelScope.launch(Dispatchers.IO) {
            loadReviewedList()
        }
    }

//    fun loadSavedList() {
//        val ids = savedPrefRepository.getSvcidList()
//        _savedList.value = ids.map { dbMemoryRepository.findBySvcid(it) }
//    }

    fun loadSavedList(svcidList: List<String>) {
        _savedList.value = svcidList.map { dbMemoryRepository.findBySvcid(it) }
    }

    fun clearSavedList() {
        savedPrefRepository.clear()
    }

    private suspend fun loadReviewedList() {
        // 서버에서 내 UUID로 내 후기 목록 가져오기

        // 서비스 아이디 없는거면 안띄움.
        /*
        서버에서는 바로 삭제하긴 그렇고,
        삭제 시간이랑 서비스가 삭제돼서 사라진건지 사용자가 삭제한건지, 신고삭제 당한건지 유형도 붙여서
        '삭제된 후기' 테이블로 옮겨두기.
        겨우 텍스트라서 거의 무한정 쌓아둘 수 있을 것 같은데, 만약 비워줄 필요가 생긴다면
        삭제 시간 오래된 순으로 혹은 일정 시간 지난 애들은 삭제되도록 해주면 됨.
         */

        val id = idPrefRepository.load()
        val reviewEntities = userRepository.getReview(id)
        _reviewedList.postValue(reviewEntities.mapNotNull { reviewEntity ->
            if (reviewEntity.svcId == null) {
                Log.e(
                    "jj-마이페이지 뷰모델",
                    "loadReviewedList - reviewEntity.svcId == null. uuid: $id"
                )
                return@mapNotNull null
            }
            val row = dbMemoryRepository.findBySvcid(reviewEntity.svcId)
                ?: return@mapNotNull null.apply {
                    Log.e(
                        "jj-마이페이지 뷰모델",
                        "loadReviewedList - dbMemoryRepository.findBySvcid == null. svcId: ${reviewEntity.svcId}"
                    )
                }
            ReviewedData(row, reviewEntity.content ?: "", reviewEntity.uploadTime ?: "")
        })
    }

    companion object {
        val factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as SeoulPublicServiceApplication).container
                MyPageViewModel(
                    savedPrefRepository = container.savedPrefRepository,
                    getDetailSeoulUseCase = container.getDetailSeoulUseCase,
                    dbMemoryRepository = container.dbMemoryRepository,
                    idPrefRepository = container.idPrefRepository,
                    userRepository = container.userRepository,
                    reviewRepository = container.reviewRepository,
                )
            }
        }
    }

}
