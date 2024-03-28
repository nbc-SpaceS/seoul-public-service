package com.wannabeinseoul.seoulpublicservice.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.naver.maps.geometry.LatLng
import com.wannabeinseoul.seoulpublicservice.SeoulPublicServiceApplication
import com.wannabeinseoul.seoulpublicservice.databases.RecentEntity
import com.wannabeinseoul.seoulpublicservice.databases.ReservationEntity
import com.wannabeinseoul.seoulpublicservice.databases.ReservationRepository
import com.wannabeinseoul.seoulpublicservice.databases.firestore.ReviewRepository
import com.wannabeinseoul.seoulpublicservice.databases.firestore.ServiceRepository
import com.wannabeinseoul.seoulpublicservice.databases.firestore.UserBanRepository
import com.wannabeinseoul.seoulpublicservice.databases.firestore.UserRepository
import com.wannabeinseoul.seoulpublicservice.pref.IdPrefRepository
import com.wannabeinseoul.seoulpublicservice.pref.RecentPrefRepository
import com.wannabeinseoul.seoulpublicservice.pref.SavedPrefRepository
import com.wannabeinseoul.seoulpublicservice.ui.dialog.review.ReviewItem
import com.wannabeinseoul.seoulpublicservice.util.DLog
import com.wannabeinseoul.seoulpublicservice.weather.WeatherShortRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class DetailViewModel(
    private val reservationRepository: ReservationRepository,
    private val idPrefRepository: IdPrefRepository,
    private val reviewRepository: ReviewRepository,
    private val userRepository: UserRepository,
    private val serviceRepository: ServiceRepository,
    private val savedPrefRepository: SavedPrefRepository,
    private val userBanRepository: UserBanRepository,
    private val recentPrefRepository: RecentPrefRepository,
    private val weatherShortRepository: WeatherShortRepository
) : ViewModel() {
    private val _serviceData = MutableLiveData<ReservationEntity>()
    val serviceData: LiveData<ReservationEntity> get() = _serviceData

    private val _distanceText = MutableLiveData<String>()
    val distanceText: LiveData<String> get() = _distanceText

    // 닫기 이벤트
    private val _closeEvent = MutableLiveData<Boolean>()
    val closeEvent: LiveData<Boolean> get() = _closeEvent

//    private val _myLocationCallback = MutableLiveData<Boolean>()
//    val myLocationCallback:LiveData<Boolean> get() = _myLocationCallback

    private val _textState = MutableLiveData<Boolean>()
    val textState: LiveData<Boolean> get() = _textState

    private val _reviewUiState: MutableLiveData<List<ReviewItem>> = MutableLiveData()
    val reviewUiState: LiveData<List<ReviewItem>> get() = _reviewUiState

    private val _savedID: MutableLiveData<Boolean> = MutableLiveData()
    val savedID: LiveData<Boolean> get() = _savedID

    private val _favoriteChanged: MutableLiveData<Boolean> = MutableLiveData()
    val favoriteChanged: LiveData<Boolean> get() = _favoriteChanged

//    private val _shortWeather: MutableLiveData<WeatherShortDTO> = MutableLiveData()
//    val shortWeather: LiveData<WeatherShortDTO> get() = _shortWeather

    fun getData(svcID: String) {
        viewModelScope.launch{
            val result = runBlocking(Dispatchers.IO) { // runBlocking으로 사용해도 됨
                reservationRepository.getService(svcID)
            }
            result.let {
                _serviceData.value = it
            }
        }
    }

    fun close(event: Boolean) {
        _closeEvent.value = event
    }

//    fun myLocationCallbackEvent(event: Boolean) {
//        _myLocationCallback.value = event
//    }

    fun textOpened(event: Boolean) {
        _textState.value = event
    }

    fun savedID(id: String) {
        _savedID.value = savedPrefRepository.contains(id)
    }

    fun changeFavorite(id: String) {
        if(savedPrefRepository.contains(id)) {
            savedPrefRepository.remove(id)
            _favoriteChanged.value = false
        } else {
            savedPrefRepository.addSvcid(id)
            _favoriteChanged.value = true
        }
    }

    fun setReviews(svcId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val data = serviceRepository.getServiceReviews(svcId)
            val banList = userBanRepository.getBanList().toMutableList().apply {
                remove(idPrefRepository.load())
            }

            _reviewUiState.postValue(data.filter { it.userId !in banList }.sortedByDescending { it.uploadTime })
        }
    }

    fun setReviews(reviewList: List<ReviewItem>) {
        _reviewUiState.value = reviewList
    }

    companion object {
        val factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as SeoulPublicServiceApplication)
                val container = application.container
                DetailViewModel(
                    reservationRepository = container.reservationRepository,
                    idPrefRepository = container.idPrefRepository,
                    reviewRepository = container.reviewRepository,
                    userRepository = container.userRepository,
                    serviceRepository = container.serviceRepository,
                    savedPrefRepository = container.savedPrefRepository,
                    userBanRepository = container.userBanRepository,
                    recentPrefRepository = container.recentPrefRepository,
                    weatherShortRepository = container.weatherShortRepository
                )
            }
        }
    }

    fun dateFormat(date: String): String {  // 날짜 포맷
        return try {
            val datePattern = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S")
            val dateTime = LocalDateTime.parse(date, formatter)
            datePattern.format(dateTime)
        } catch (dtpe: DateTimeParseException) {
            DLog.e("DetailViewModel","Error! dateFormat", dtpe)
            date
        }
    }

    // 두 지점 간의 직선 거리를 계산하는 함수
    fun distance(point1: LatLng, point2: LatLng): Double {
        val R = 6371 // 지구의 반지름 (단위: km)
        val latDistance = Math.toRadians(point2.latitude - point1.latitude)
        val lonDistance = Math.toRadians(point2.longitude - point1.longitude)
        val a = sin(latDistance / 2) * sin(latDistance / 2) +
                (cos(Math.toRadians(point1.latitude)) * cos(Math.toRadians(point2.latitude)) *
                        sin(lonDistance / 2) * sin(lonDistance / 2))
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c * 1000 // 단위를 미터로 변환
    }

    // 반환값은 이제 사용 안할 겁니다. _distanceText에 넣어주고 옵저빙해서 텍스트뷰에 반영.
    fun distanceCheckResponse(distance : Double): String {
        return when {
            distance/1000 < 1 -> "현위치로부터 ${String.format("%.0f", distance)}m"
            distance/1000 >= 1 -> "현위치로부터 ${String.format("%.1f", distance/1000)}km"
            else -> {
                DLog.w("jj-상세페이지 뷰모델", "distanceCheckResponse distance error: $distance")
                "현위치로부터 ?km"
            }
        }
            .also {
                _distanceText.value = it
            }
    }

    fun dateFormatRecent(date: LocalDateTime): String { // RecentEntity에서 사용할 날짜 타입 변경
        return date.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
    }

    fun saveData(data: RecentEntity) {
        recentPrefRepository.setRecent(data)
    }
}