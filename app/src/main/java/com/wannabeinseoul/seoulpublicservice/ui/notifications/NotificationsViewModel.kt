package com.wannabeinseoul.seoulpublicservice.ui.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wannabeinseoul.seoulpublicservice.SeoulPublicServiceApplication
import com.wannabeinseoul.seoulpublicservice.databases.ReservationRepository
import com.wannabeinseoul.seoulpublicservice.pref.SavedPrefRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class NotificationsViewModel(
    private val savedPrefRepository: SavedPrefRepository,
    private val reservationRepository: ReservationRepository,
) : ViewModel() {

    private val _uiState: MutableLiveData<List<NotificationInfo>> = MutableLiveData()
    val uiState: LiveData<List<NotificationInfo>> get() = _uiState

    fun updateUiState() {
        viewModelScope.launch(Dispatchers.IO) {
            val datePattern = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val datePattern2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH")
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S")

            val savedServiceList = savedPrefRepository.getSvcidList().map {
                reservationRepository.getService(it)
            }

            // 예약 시작까지 하루 남은 서비스의 개수
            val list = savedServiceList.filter {
                datePattern2.format(
                    LocalDateTime.parse(
                        it.RCPTBGNDT,
                        formatter
                    )
                ) >= datePattern2.format(
                    LocalDateTime.now().plusDays(1)
                ) && datePattern.format(
                    LocalDateTime.parse(
                        it.RCPTBGNDT,
                        formatter
                    )
                ) == datePattern.format(
                    LocalDateTime.now().plusDays(1)
                )
            }.map {
                NotificationInfo(
                    it.SVCID,
                    it.SVCNM,
                    it.RCPTBGNDT,
                    "예약시작 하루전 알림"
                )
            }

            // 예약 마감까지 하루 남은 서비스의 개수
            val list2 = savedServiceList.filter {
                datePattern2.format(
                    LocalDateTime.parse(
                        it.RCPTENDDT,
                        formatter
                    )
                ) >= datePattern2.format(
                    LocalDateTime.now().plusDays(1)
                ) && datePattern.format(
                    LocalDateTime.parse(
                        it.RCPTENDDT,
                        formatter
                    )
                ) == datePattern.format(
                    LocalDateTime.now().plusDays(1)
                )
            }.map {
                NotificationInfo(
                    it.SVCID,
                    it.SVCNM,
                    it.RCPTENDDT,
                    "예약마감 하루전 알림"
                )
            }

            // 예약 가능한 서비스의 개수
            val list3 = savedServiceList.filter {
                datePattern.format(
                    LocalDateTime.parse(
                        it.RCPTBGNDT,
                        formatter
                    )
                ) == datePattern.format(
                    LocalDateTime.now()
                )
            }.map {
                NotificationInfo(
                    it.SVCID,
                    it.SVCNM,
                    it.RCPTBGNDT,
                    "예약가능 알림"
                )
            }

            _uiState.postValue((list + list2 + list3).sortedByDescending { it.date })
        }
    }


    companion object {
        /** 뷰모델팩토리에서 의존성주입을 해준다 */
        val factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as SeoulPublicServiceApplication)
                val container = application.container
                NotificationsViewModel(
                    savedPrefRepository = container.savedPrefRepository,
                    reservationRepository = container.reservationRepository,
                )
            }
        }
    }

}
