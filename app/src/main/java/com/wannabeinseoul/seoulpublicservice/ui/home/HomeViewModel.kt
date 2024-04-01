package com.wannabeinseoul.seoulpublicservice.ui.home

import android.util.Log
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wannabeinseoul.seoulpublicservice.SeoulPublicServiceApplication
import com.wannabeinseoul.seoulpublicservice.databases.RecentEntity
import com.wannabeinseoul.seoulpublicservice.databases.ReservationEntity
import com.wannabeinseoul.seoulpublicservice.databases.ReservationRepository
import com.wannabeinseoul.seoulpublicservice.databases.firestore.WeatherDBRepository
import com.wannabeinseoul.seoulpublicservice.db_by_memory.DbMemoryRepository
import com.wannabeinseoul.seoulpublicservice.kma.midLandFcst.Item
import com.wannabeinseoul.seoulpublicservice.kma.midLandFcst.KmaRepository
import com.wannabeinseoul.seoulpublicservice.kma.midTemp.TempRepository
import com.wannabeinseoul.seoulpublicservice.pref.RecentPrefRepository
import com.wannabeinseoul.seoulpublicservice.pref.RegionPrefRepository
import com.wannabeinseoul.seoulpublicservice.pref.SavedPrefRepository
import com.wannabeinseoul.seoulpublicservice.pref.SearchPrefRepository
import com.wannabeinseoul.seoulpublicservice.util.DLog
import com.wannabeinseoul.seoulpublicservice.weather.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class HomeViewModel(
    private val regionPrefRepository: RegionPrefRepository,
    private val searchPrefRepository: SearchPrefRepository,
    private val reservationRepository: ReservationRepository,
    private val dbMemoryRepository: DbMemoryRepository,
    private val savedPrefRepository: SavedPrefRepository,
    private val recentPrefRepository: RecentPrefRepository,
    private val weatherShortRepository: WeatherShortRepository,
    private val kmaRepository: KmaRepository,
    private val tempRepository: TempRepository,
    private val weatherDBRepository: WeatherDBRepository
) : ViewModel() {

    private var selectedRegions: List<String> = emptyList()

    private var _randomService: List<String> = emptyList()
    val randomService: List<String> get() = _randomService

    private var _notificationSign: MutableLiveData<Boolean> = MutableLiveData()
    val notificationSign: MutableLiveData<Boolean> get() = _notificationSign

    private val _updateSelectedRegions: MutableLiveData<List<String>> = MutableLiveData()
    val updateSelectedRegions: LiveData<List<String>> get() = _updateSelectedRegions

    private var _displaySearchResult: MutableLiveData<List<ReservationEntity>> = MutableLiveData()
    val displaySearchResult: LiveData<List<ReservationEntity>> get() = _displaySearchResult

    private var _displaySearchHistory: MutableLiveData<Pair<List<String>, SearchPrefRepository>> =
        MutableLiveData()
    val displaySearchHistory: LiveData<Pair<List<String>, SearchPrefRepository>> get() = _displaySearchHistory

    private val _recentData: MutableLiveData<List<RecentEntity>> = MutableLiveData()
    val recentData: LiveData<List<RecentEntity>> get() = _recentData

    private val _updateViewPagerCategory: MutableLiveData<List<Pair<String, Int>>> =
        MutableLiveData()
    val updateViewPagerCategory: LiveData<List<Pair<String, Int>>> get() = _updateViewPagerCategory

    private val _shortWeather: MutableLiveData<List<WeatherShort>> = MutableLiveData()
    val shortWeather: LiveData<List<WeatherShort>> get() = _shortWeather

    private val _weatherData: MutableLiveData<List<WeatherShort>> = MutableLiveData()
    val weatherData: LiveData<List<WeatherShort>> get() = _weatherData

    private val _mediatorLiveData: MutableLiveData<List<WeatherShort>> = MutableLiveData()
    val mediatorLiveData: LiveData<List<WeatherShort>> get() = _mediatorLiveData

    // 검색 결과를 지우는 메소드
    fun clearSearchResult() {
        if (_displaySearchResult.value?.isNotEmpty() == true) _displaySearchResult.value =
            emptyList()
        if (_displaySearchHistory.value?.first?.isNotEmpty() == true) _displaySearchHistory.value =
            Pair(
                emptyList(), searchPrefRepository
            )
    }

    // 뷰페이저 카테고리 설정 메소드
    fun setViewPagerCategory(area: String) {
        _updateViewPagerCategory.value = dbMemoryRepository.getFilteredCountWithMaxClass(
            listOf(
                "체육시설",
                "교육강좌",
                "문화체험",
                "공간시설",
                "진료복지"
            ), area
        ).filter { it.second != 0 }
    }

    // 랜덤 서비스 설정 메소드
    fun setRandomService() {
        _randomService = dbMemoryRepository.getFilteredByDate()
    }

    // 지역 설정 메소드
    fun setupRegions() {
        selectedRegions = regionPrefRepository.load().toMutableList()

        when {
            selectedRegions.isNotEmpty() -> {
                regionPrefRepository.saveSelectedRegion(1)
                _updateSelectedRegions.value = selectedRegions
            }

            else -> {
                _updateSelectedRegions.value = emptyList()
            }
        }
    }

    // 검색어를 이용하여 검색을 수행하는 메소드
    fun performSearch(query: String) {
        // 검색어가 비어있지 않을 때만 검색어가 저장됨
        if (query.isNotEmpty()) {
            saveSearchQuery(query)
            Log.d("Search", "Saved search query: $query")
        }

        viewModelScope.launch(Dispatchers.IO) {
            displaySearchResults(query)
        }
    }

    // 검색어 저장 메소드
    private fun saveSearchQuery(query: String) {
        searchPrefRepository.save(query)
        Log.d("Search", "Saved search query: $query") // 로그 찍기
    }

    // 검색 결과를 가져오는 메소드
    private suspend fun displaySearchResults(query: String) {
        // searchText 메소드를 호출하여 검색 결과를 가져옴
        val answer = reservationRepository.searchText(query)
        if(answer.isEmpty()) _displaySearchResult.postValue(emptyList())
        else _displaySearchResult.postValue(answer)
    }

    // 검색어 목록 불러오기
    fun showSearchHistory() {
        // 포커스가 EditText에 있을 때 저장된 검색어를 불러옴
        _displaySearchHistory.value =
            Pair(searchPrefRepository.load().toMutableList(), searchPrefRepository)
    }

    // 선택된 지역을 저장하는 메소드
    fun saveSelectedRegion(index: Int) {
        regionPrefRepository.saveSelectedRegion(index)
    }

    // 새 알림 표시 업데이트
    fun updateNotificationSign() {
        if (savedPrefRepository.getFlag().not()) {
            savedPrefRepository.setFlag(true)
            viewModelScope.launch(Dispatchers.IO) {
                val datePattern = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S")

                val savedServiceList = savedPrefRepository.getSvcidList().map {
                    reservationRepository.getService(it)
                }

                // 예약 시작까지 하루 남은 서비스의 개수
                val list = savedServiceList.filter {
                    datePattern.format(
                        LocalDateTime.parse(
                            it.RCPTBGNDT,
                            formatter
                        )
                    ) > datePattern.format(
                        LocalDateTime.now()
                    ) && datePattern.format(
                        LocalDateTime.parse(
                            it.RCPTBGNDT,
                            formatter
                        )
                    ) < datePattern.format(
                        LocalDateTime.now().plusDays(2)
                    )
                }.size

                // 예약 마감까지 하루 남은 서비스의 개수
                val list2 = savedServiceList.filter {
                    datePattern.format(
                        LocalDateTime.parse(
                            it.RCPTENDDT,
                            formatter
                        )
                    ) < datePattern.format(
                        LocalDateTime.now()
                    ) && datePattern.format(
                        LocalDateTime.parse(
                            it.RCPTENDDT,
                            formatter
                        )
                    ) > datePattern.format(
                        LocalDateTime.now().minusDays(2)
                    )
                }.size

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
                }.size

                _notificationSign.postValue(list != 0 || list2 != 0 || list3 != 0)
            }
        }
    }

    // 새 알림 표시 가리기
    fun hideNotificationSign() {
        _notificationSign.value = false
    }

    // 최근 서비스 목록 불러오기
    fun loadRecentData() {
        _recentData.value = recentPrefRepository.getRecent()
    }

    // 날씨 정보를 가져오는 메소드
    fun fetchWeatherData() {
        viewModelScope.launch {
            val now = LocalDateTime.now()
            val hour = now.hour

            val tmFc: String = if (hour < 6) {
                // 현재 시간이 06시 이전인 경우, 이전 날짜의 18시를 설정
                now.minusDays(1).withHour(18).withMinute(0).withSecond(0)
                    .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"))
            } else if (hour < 18) {
                // 현재 시간이 06시와 18시 사이인 경우, 당일의 06시를 설정
                now.withHour(6).withMinute(0).withSecond(0)
                    .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"))
            } else {
                // 현재 시간이 18시 이후인 경우, 당일의 18시를 설정
                now.withHour(18).withMinute(0).withSecond(0)
                    .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"))
            }
            try {
                val response =
                    if (WeatherData.getMid() == null || WeatherData.getDate() != LocalDate.now().dayOfMonth) {
                        kmaRepository.getMidLandFcst(      // 중기예보(error = emptyList)
                            numOfRows = 10,
                            pageNo = 1,
                            dataType = "JSON",
                            regId = "11B00000",
                            tmFc = tmFc
                        )
                    } else {
                        WeatherData.getMid()
                    }
                val responseTemp =
                    if (WeatherData.getTmp() == null || WeatherData.getDate() != LocalDate.now().dayOfMonth) {
                        tempRepository.getTemp(         // 중기기온
                            numOfRows = 10,
                            pageNo = 1,
                            dataType = "JSON",
                            regId = "11B10101",
                            tmFc = tmFc
                        )
                    } else {
                        WeatherData.getTmp()
                    }
                if (response != null && responseTemp != null) {
                    DLog.i(
                        "This is HomeViewModel",
                        "kma : ${response}\ntemp : $responseTemp"
                    )
                    setWeatherShort(
                        response,
                        responseTemp
                    )
                } else {
                    throw Exception("else) response != null && responseTemp != null")
                }
            } catch (e: Throwable) {
                DLog.e("HomeViewModel", "fetchWeatherData error", e)
                _weatherData.postValue(emptyList())
            }
        }
    }

    suspend fun checkWeatherFromDB(region: String): List<WeatherShort>? = weatherDBRepository.getWeather(region)
    suspend fun setWeatherToDB(list: List<WeatherShort>) {
        weatherDBRepository.setWeather(regionPrefRepository.loadSelectedRegion(), list)
    }
    suspend fun getWeatherUpdateTimeFromDB(region: String): Long? = weatherDBRepository.getUpdateTime(region)

    fun setMediatorLiveData(list: List<WeatherShort>) {
        _mediatorLiveData.postValue(list)
    }

    // 날씨 단기 예보 가져오기
    fun weatherShortData(lat_x: Int, lng_y: Int) {
        if (lat_x != Int.MAX_VALUE && lng_y != Int.MAX_VALUE) {
            viewModelScope.launch(Dispatchers.IO) { // 여기서부터 실행해야함
                val run = runBlocking(Dispatchers.IO) {
                    val locale = ZoneId.of("Asia/Seoul")
                    val local = LocalDateTime.now(locale)
                    var y = local.year
                    var m = String.format("%02d", local.monthValue)
                    var d = local.dayOfMonth
                    var h = local.hour
                    DLog.i("This is HomeViewModel", "m : $m\nd : $d\nh : $h")
                    if (h < 5) {
                        val yesterday = local.minusDays(1)
                        d = yesterday.dayOfMonth
                        h = 17
                        if (d == 1) {
                            if (local.month.value == 1) {
                                m = "12"
                                val lastYear = local.minusYears(1)
                                y = lastYear.year
                                d = lastYear.month.maxLength()
                            } else {
                                val lastMonth = yesterday.minusMonths(1)
                                m = String.format("%02d", lastMonth.monthValue)
                                d = lastMonth.month.maxLength()
                            }
                        }
                    } else {
                        h = 5
                    }
                    DLog.i("This is HomeViewModel", "m : $m\nh : $h")
                    val localDate = "$y$m${String.format("%02d", d)}"
                    val localTime = "${String.format("%02d", h)}10"
                    DLog.i(
                        "This is HomeViewModel",
                        "localDate : $localDate\nlocalTime : $localTime\nlat_x : $lat_x\nlng_y : $lng_y"
                    )
                    weatherShortRepository.getShortWeather(
                        1,
                        1000,
                        localDate,
                        localTime,
                        lat_x,
                        lng_y
                    )
                }
                run.let {
                    val itemList = mutableListOf<WeatherShort>()
                    val items = it
                    var skyValue: Int? = null
                    var tmpValue: Int? = null
                    var popValue: Int? = null
                    val filtering =
                        items.filter { it.fcstTime == "0600" && (it.category == "SKY" || it.category == "TMP" || it.category == "POP") }
                    for (item in filtering) {
                        if (item.category == "POP") popValue = item.fcstValue?.toIntOrNull() ?: -1
                        if (item.category == "SKY") skyValue =
                            item.fcstValue?.toIntOrNull() ?: 4  // null 이면 흐림을 기본값으로
                        if (item.category == "TMP") tmpValue = item.fcstValue?.toIntOrNull() ?: 99
                        DLog.i(
                            "This is HomeViewModel",
                            "skyValue : $skyValue\ntmpValue : $tmpValue\npopValue : $popValue"
                        )
                        if (skyValue != null && tmpValue != null && popValue != null) {
                            itemList.add(WeatherShort(skyValue, tmpValue, popValue))
                            skyValue = null
                            tmpValue = null
                            popValue = null
                        }
                    }
                    WeatherData.saveMix(itemList)
                    DLog.i("This is HomeViewModel", "itemList count : ${itemList.count()}")
                    _shortWeather.postValue(itemList)
                }
            }
        } else {
            _shortWeather.postValue(WeatherData.getMix())
        }
    }

    private fun setWeatherShort(
        dto: Item,
        temp: com.wannabeinseoul.seoulpublicservice.kma.midTemp.Item
    ) {
        val itemList = mutableListOf<WeatherShort>()
        dto.let {
            itemList.add(
                ShortMidMapper.midToShort(
                    WeatherMid(
                        it.wf3Am ?: "",
                        ((temp.taMax3 ?: 99) + (temp.taMin3 ?: 99)) / 2,
                        it.rnSt3Am ?: -1
                    )
                )
            )
            itemList.add(
                ShortMidMapper.midToShort(
                    WeatherMid(
                        it.wf4Am ?: "",
                        ((temp.taMax4 ?: 99) + (temp.taMin4 ?: 99)) / 2,
                        it.rnSt4Am ?: -1
                    )
                )
            )
            itemList.add(
                ShortMidMapper.midToShort(
                    WeatherMid(
                        it.wf5Am ?: "",
                        ((temp.taMax5 ?: 99) + (temp.taMin5 ?: 99)) / 2,
                        it.rnSt5Am ?: -1
                    )
                )
            )
            itemList.add(
                ShortMidMapper.midToShort(
                    WeatherMid(
                        it.wf6Am ?: "",
                        ((temp.taMax6 ?: 99) + (temp.taMin6 ?: 99)) / 2,
                        it.rnSt6Am ?: -1
                    )
                )
            )
            itemList.add(
                ShortMidMapper.midToShort(
                    WeatherMid(
                        it.wf7Am ?: "",
                        ((temp.taMax7 ?: 99) + (temp.taMin7 ?: 99)) / 2,
                        it.rnSt7Am ?: -1
                    )
                )
            )
            itemList.add(
                ShortMidMapper.midToShort(
                    WeatherMid(
                        it.wf8 ?: "",
                        ((temp.taMax8 ?: 99) + (temp.taMin8 ?: 99)) / 2,
                        it.rnSt8 ?: -1
                    )
                )
            )
            itemList.add(
                ShortMidMapper.midToShort(
                    WeatherMid(
                        it.wf9 ?: "",
                        ((temp.taMax9 ?: 99) + (temp.taMin9 ?: 99)) / 2,
                        it.rnSt9 ?: -1
                    )
                )
            )
            itemList.add(
                ShortMidMapper.midToShort(
                    WeatherMid(
                        it.wf10 ?: "",
                        ((temp.taMax10 ?: 99) + (temp.taMin10 ?: 99)) / 2,
                        it.rnSt10 ?: -1
                    )
                )
            )
        }
        _weatherData.postValue(itemList)
    }

    companion object {
        val factory = viewModelFactory {
            initializer {
                val application =
                    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SeoulPublicServiceApplication)
                val container = application.container
                HomeViewModel(
                    regionPrefRepository = container.regionPrefRepository,
                    searchPrefRepository = container.searchPrefRepository,
                    reservationRepository = container.reservationRepository,
                    dbMemoryRepository = container.dbMemoryRepository,
                    savedPrefRepository = container.savedPrefRepository,
                    recentPrefRepository = container.recentPrefRepository,
                    kmaRepository = container.kmaRepository,
                    weatherShortRepository = container.weatherShortRepository,
                    tempRepository = container.tempRepository,
                    weatherDBRepository = container.weatherDBRepository
                )
            }
        }
    }
}