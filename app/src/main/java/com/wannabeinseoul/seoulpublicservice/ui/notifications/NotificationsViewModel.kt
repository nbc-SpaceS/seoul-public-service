package com.wannabeinseoul.seoulpublicservice.ui.notifications

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wannabeinseoul.seoulpublicservice.SeoulPublicServiceApplication
import com.wannabeinseoul.seoulpublicservice.databases.ReservationEntity
import com.wannabeinseoul.seoulpublicservice.databases.ReservationRepository
import com.wannabeinseoul.seoulpublicservice.seoul.Row
import com.wannabeinseoul.seoulpublicservice.usecase.GetAll2000UseCase
import com.wannabeinseoul.seoulpublicservice.usecase.GetDetailSeoulUseCase
import com.wannabeinseoul.seoulpublicservice.util.RoomRowMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random

class NotificationsViewModel(
    private val getAll2000UseCase: GetAll2000UseCase,
    private val reservationRepository: ReservationRepository,
    private val getDetailSeoulUseCase: GetDetailSeoulUseCase
) : ViewModel() {

    private val _text: MutableLiveData<String> =
        MutableLiveData("_text 라이브데이터 초기값 ㅁㄴㅇㄹㅁㄴㅇㄹ")
    val text: LiveData<String> = _text

    private val _isBtnEnabled: MutableLiveData<Boolean> =
        MutableLiveData(true)
    val isBtnEnabled: LiveData<Boolean> = _isBtnEnabled

    private var rowList: List<Row> = emptyList()
    private val random = Random

    /**
     * @property reservationList [ReservationEntity] 타입의 List 변수
     */
    private var reservationList: List<ReservationEntity> = emptyList()
    fun setRandomOne() {
        Log.i("This is NotifiViewModel","setRandomOne() Start!")
        _isBtnEnabled.value = false
        // getAll2000UseCase 처리 시 1600개 정도 데이터를 변환하고 반환하는 과정이 cpu가 좀 필요할 듯 해서 Default로 줌.
        val job = if (rowList.isEmpty()) viewModelScope.launch(Dispatchers.Default) {

            for (rowItem in getAll2000UseCase()) {
                reservationList += RoomRowMapper.mappingRowToRoom(rowItem)
            }
            Log.i("This is NotifiViewModel", "reserve count : ${reservationList.count()}")
            reservationRepository.insertAll(reservationList)
            for (reservation in reservationRepository.getAll()) {
                rowList += RoomRowMapper.mappingRoomToRow(reservation)
            }
            // 필터 테스트 용
            val itemSmall: List<String> = listOf("어린이병원")
            val itemLocate: List<String> = listOf("서초구")
            val itemState: List<String> = listOf("")
            val itemPay: List<String> = listOf("")
            val testList = reservationRepository.getFilter(itemSmall, itemLocate, itemState, itemPay)
            Log.i("This is NVM","\n소분류 : $itemSmall\n지역구 : $itemLocate\n접수중 : $itemState\n가 격 : $itemPay\n목록 개수 : ${testList.size}")

            val row = rowList.firstOrNull()
            if (row == null) {
                _text.postValue("rowList.size: ${rowList.size}\n\nrowList empty")
                Log.w("jj-노티뷰모델", "rowList is empty")
            } else {
                _text.postValue("rowList.size: ${rowList.size}\n\n${getDetailSeoulUseCase(row.svcid)}")
                Log.d("jj-노티뷰모델", "id: ${row.svcid}")
            }
        }
        else viewModelScope.launch(Dispatchers.IO) {
            val row = rowList[random.nextInt(0, rowList.size)]
            val detailRow = getDetailSeoulUseCase(row.svcid)
            if (detailRow == null) {
                _text.postValue("detailRow == null\n\nrowList.size: ${rowList.size}\n\n$row")
            } else {
                _text.postValue("rowList.size: ${rowList.size}\n\n$detailRow")
            }
            Log.d("jj-노티뷰모델", "id: ${row.svcid}")
        }

        viewModelScope.launch(Dispatchers.IO) {
            job.join()
            _isBtnEnabled.postValue(true)
        }
    }

    companion object {
        /** 뷰모델팩토리에서 의존성주입을 해준다 */
        val factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as SeoulPublicServiceApplication)
                val container = application.container
                NotificationsViewModel(
                    getAll2000UseCase = container.getAll2000UseCase,
                    reservationRepository = container.reservationRepository,
                    getDetailSeoulUseCase = container.getDetailSeoulUseCase
                )
            }
        }
    }

}
