package com.wannabeinseoul.seoulpublicservice.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.wannabeinseoul.seoulpublicservice.R
import com.wannabeinseoul.seoulpublicservice.databases.RecentEntity
import com.wannabeinseoul.seoulpublicservice.databinding.FragmentHomeBinding
import com.wannabeinseoul.seoulpublicservice.ui.category.CategoryItemClick
import com.wannabeinseoul.seoulpublicservice.ui.detail.DetailCloseInterface
import com.wannabeinseoul.seoulpublicservice.ui.detail.DetailFragment
import com.wannabeinseoul.seoulpublicservice.ui.interestregionselect.InterestRegionSelectActivity
import com.wannabeinseoul.seoulpublicservice.ui.main.MainViewModel
import com.wannabeinseoul.seoulpublicservice.ui.main.adapter.HomeSearchAdapter
import com.wannabeinseoul.seoulpublicservice.ui.main.adapter.SearchHistoryAdapter
import com.wannabeinseoul.seoulpublicservice.ui.notifications.NotificationsFragment
import com.wannabeinseoul.seoulpublicservice.weather.WeatherAdapter
import com.wannabeinseoul.seoulpublicservice.weather.WeatherData
import com.wannabeinseoul.seoulpublicservice.weather.WeatherSeoulArea
import com.wannabeinseoul.seoulpublicservice.weather.WeatherShort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by viewModels { HomeViewModel.factory }
    private val mainViewModel: MainViewModel by activityViewModels()

    private var backPressedOnce = false

    private var resultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            homeViewModel.setupRegions()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViewModel()
        setupUIComponents()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ViewModel, LiveData 초기화 설정
    private fun initViewModel() {
        // MainViewModel의 LiveData를 관찰하여 UI를 업데이트
        mainViewModel.selectRegion.observe(viewLifecycleOwner) {
            if (it != "지역선택") {
                homeViewModel.setViewPagerCategory(it)
                weatherDataSend(it)  // 지역정보를 기상청 좌표로 변환한 후 API를 요청하기 위해(단기예보)
            } else {
                binding.tvHomeDescription.text = "아직 관심지역이 선택되지 않았습니다."
            }
        }

        mainViewModel.moveSelectRegions.observe(viewLifecycleOwner) {
            if (it) {
                val intent = Intent(context, InterestRegionSelectActivity::class.java)
                resultLauncher.launch(intent)
            }
        }

        // HomeViewModel의 LiveData를 관찰하여 UI를 업데이트
        with(homeViewModel) {
            updateSelectedRegions.observe(viewLifecycleOwner) { selectedRegions ->
                with(binding) {
                    if (selectedRegions.isEmpty()) {
                        tvHomeCurrentRegion.text = "지역선택"
                        tvHomeSelectRegion1.isVisible = false
                        tvHomeSelectRegion2.isVisible = false
                        tvHomeSelectRegion3.isVisible = false
                        mdHomeRegionList.isVisible = false
                        etSearch.hint = "전체 공공서비스 대상 검색"
                        mainViewModel.setRegion("지역선택")
                    } else {
                        tvHomeCurrentRegion.text = selectedRegions[0]
                        tvHomeSelectRegion1.setTextColor(requireContext().getColor(R.color.total_text_color))
                        mdHomeRegionList.isVisible = true
                        etSearch.hint = "${selectedRegions[0]} 공공서비스 대상 검색"
                        mainViewModel.setRegion(selectedRegions[0])
                        when (selectedRegions.size) {
                            1 -> {
                                tvHomeSelectRegion1.text = selectedRegions[0]
                                tvHomeSelectRegion1.isVisible = true
                                tvHomeSelectRegion2.isVisible = false
                                tvHomeSelectRegion3.isVisible = false
                            }

                            2 -> {
                                tvHomeSelectRegion1.text = selectedRegions[0]
                                tvHomeSelectRegion2.text = selectedRegions[1]
                                tvHomeSelectRegion1.isVisible = true
                                tvHomeSelectRegion2.isVisible = true
                                tvHomeSelectRegion3.isVisible = false
                            }

                            3 -> {
                                tvHomeSelectRegion1.text = selectedRegions[0]
                                tvHomeSelectRegion2.text = selectedRegions[1]
                                tvHomeSelectRegion3.text = selectedRegions[2]
                                tvHomeSelectRegion1.isVisible = true
                                tvHomeSelectRegion2.isVisible = true
                                tvHomeSelectRegion3.isVisible = true
                            }
                        }
                    }
                }
            }

            displaySearchResult.observe(viewLifecycleOwner) { searchResult ->
                with(binding) {
                    if (searchResult.isNotEmpty()) {
                        // 검색 결과를 HomeSearchAdapter에 전달하여 RecyclerView에 표시
                        val adapter = HomeSearchAdapter(searchResult)
                        rvSearchResults.adapter = adapter
                        rvSearchResults.layoutManager = LinearLayoutManager(requireContext())

                        // 검색을 수행할 때 cl_home_region_list를 숨김
                        clHomeRegionList.isVisible = false

                        // 검색 결과를 표시하는 RecyclerView를 보이게 함
                        clSearchResults.isVisible = true
                        tvHomeEmptyDescription.isVisible = false

                    // 키보드 숨기기
                    hideKeyboard()

                    // et_search 포커스 제거
                    etSearch.clearFocus()

                    // 클릭된 결과 아이템의 SVCID를 상세 페이지에 전달
                    searchClick(adapter)
                    } else {
                        clSearchResults.isVisible = true
                        tvHomeEmptyDescription.isVisible = true

                        // 키보드 숨기기
                        hideKeyboard()

                        // et_search 포커스 제거
                        etSearch.clearFocus()
                    }
                }
            }

            displaySearchHistory.observe(viewLifecycleOwner) { searchHistory ->
                with(binding) {
                    if (searchHistory.first.isNotEmpty()) {
                        // 검색어를 SearchHistoryAdapter에 전달하여 RecyclerView에 표시
                        val adapter = SearchHistoryAdapter(
                            searchHistory.first.toMutableList(),
                            searchHistory.second
                        ).apply {
                            onItemClickListener = object : SearchHistoryAdapter.OnItemClickedListener {
                                override fun onItemClick(item: String) {
                                    etSearch.setText(item)
                                    homeViewModel.performSearch(tvHomeCurrentRegion.text.toString(), item)
                                }
                            }
                        }
                        rvSearchHistory.adapter = adapter
                        rvSearchHistory.layoutManager = LinearLayoutManager(requireContext())

                        // 검색어 저장 목록을 표시하는 RecyclerView를 보이게 함
                        rvSearchHistory.visibility = View.VISIBLE
                    }
                }
            }

            updateViewPagerCategory.observe(viewLifecycleOwner) {
                binding.tvHomeDescription.text = when (it.size) {
                    0 -> "${mainViewModel.selectRegion.value}에는 사용할 수 있는 서비스가 없습니다."
                    1 -> "${mainViewModel.selectRegion.value}에는 ${it[0].first} 서비스가 있습니다."
                    2 -> "${mainViewModel.selectRegion.value}에는 ${it[0].first}, ${it[1].first} 서비스가 있습니다."
                    3 -> "${mainViewModel.selectRegion.value}에는 ${it[0].first}, ${it[1].first}, ${it[2].first} 서비스가 있습니다."
                    4 -> "${mainViewModel.selectRegion.value}에는 ${it[0].first}, ${it[1].first}, ${it[2].first}, ${it[3].first} 서비스가 있습니다."
                    else -> "${mainViewModel.selectRegion.value}에서 모든 서비스를 사용할 수 있습니다."
                }
                binding.tvHomeDescription.text = when (it.size) {
                    1 -> setSpannableString(5, 10)
                    2 -> setSpannableString(5, 16)
                    3 -> setSpannableString(5, 22)
                    4 -> setSpannableString(5, 28)
                    else -> binding.tvHomeDescription.text
                }
            }

            notificationSign.observe(viewLifecycleOwner) {
                binding.ivHomeNotificationCountBackground.isVisible = it
            }

            loadRecentData()
            recentData.observe(viewLifecycleOwner) {
                setupRecentData()
                recentViewPager(it)
            }
            shortWeather.observe(viewLifecycleOwner) {      // 단기예보
                if (!weatherData.value.isNullOrEmpty()) {
                    val weatherDataList = weatherData.value
                    if (!it.isNullOrEmpty() && !weatherDataList.isNullOrEmpty()) {
                        val combinedData = it + weatherDataList
                        setMediatorLiveData(combinedData)
                    }
                }
            }
            weatherData.observe(viewLifecycleOwner) { weatherData ->        // 중기예보(기온 포함됨)
                if (!shortWeather.value.isNullOrEmpty()) {
                    val shortWeatherList = shortWeather.value
                    if (!weatherData.isNullOrEmpty() && !shortWeatherList.isNullOrEmpty()) {
                        val combinedData = shortWeatherList + weatherData
                        setMediatorLiveData(combinedData)
                    }
                }
            }
            mediatorLiveData.observe(viewLifecycleOwner) {
                if (it.isNotEmpty()) {
                    weatherAdapter(it)
                    binding.tvHomeWeatherForecast.isVisible = true
                    binding.tvHomeWeatherForecastDescription.isVisible = true
                }
            }
        }
    }

    // UI 구성 요소 설정
    private fun setupUIComponents() {
        homeViewModel.setupRegions()
        homeViewModel.updateNotificationSign()
        homeViewModel.setRandomService()

        setupViewPager()
        setupBackPress()
        setupSearch()
        setupSearchHistory()
        setupRegionSelection()
        setupNotificationClick()
        setupBannerClick()
    }

    // ViewPager, TabLayout 설정
    private fun setupViewPager() {
        val viewPager = binding.viewPager
        val tabLayout = binding.tabLayout

        viewPager.offscreenPageLimit = 2
        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 5

            override fun createFragment(position: Int): Fragment {
                val fragment = when (position) {
                    0 -> FacilityFragment()
                    1 -> EducationFragment()
                    2 -> CultureEventFragment()
                    3 -> FacilityRentFragment()
                    4 -> MedicalFragment()
                    else -> Fragment()
                }
                return fragment
            }
        }

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "체육시설"
                1 -> tab.text = "교육강좌"
                2 -> tab.text = "문화체험"
                3 -> tab.text = "공간시설"
                4 -> tab.text = "진료복지"
            }
        }.attach()
    }

    // 뒤로 가기 버튼 설정
    private fun setupBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // RecyclerView가 보일 때만 ViewPager, TabLayout을 보이게 하고, RecyclerView를 숨김
                    if (binding.clSearchResults.visibility == View.VISIBLE) {
                        // 뒤로 가기 버튼을 누를 때 cl_home_region_list를 숨깁니다.
                        binding.clHomeRegionList.isVisible = false
                        binding.tvServiceList.visibility = View.VISIBLE
                        binding.viewPager.visibility = View.VISIBLE
                        binding.tabLayout.visibility = View.VISIBLE
                        binding.clSearchResults.isVisible = false
                        binding.tvHomeEmptyDescription.isVisible = false
                    } else if (backPressedOnce) {
                        isEnabled = false
                        requireActivity().finish()
                    } else {
                        backPressedOnce = true
                        Toast.makeText(requireContext(), "한번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT)
                            .show()

                        Handler(Looper.getMainLooper()).postDelayed({
                            backPressedOnce = false
                        }, 2000)
                    }
                }
            }
        )
    }

    // 검색 기능 설정
    private fun setupSearch() {
        val region = binding.tvHomeCurrentRegion.text.toString()

        binding.ivSearch.setOnClickListener {
            val searchText = binding.etSearch.text.toString()
            homeViewModel.performSearch(region, searchText)
        }

        binding.etSearch.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val searchText = v.text.toString()
                homeViewModel.performSearch(region, searchText)

                // EditText의 포커스 제거
                binding.etSearch.clearFocus()
                true
            } else {
                false
            }
        }
    }

    // 검색어 저장 목록을 설정
    @SuppressLint("ClickableViewAccessibility")
    private fun setupSearchHistory() {
        binding.etSearch.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                homeViewModel.showSearchHistory()
                binding.viewControlRvSearchResults.visibility = View.VISIBLE
            } else {
                hideSearchHistory()
                binding.viewControlRvSearchResults.visibility = View.GONE
            }
        }

        // viewControlRvSearchResults가 터치될 때 호출되는 리스너 설정
        binding.viewControlRvSearchResults.setOnTouchListener { _, _ ->
            // viewControlRvSearchResults가 터치되면 EditText의 포커스를 해제, 키보드 숨기기
            binding.etSearch.clearFocus()
            hideKeyboard()
            true
        }
    }

    private fun setupRegionSelection() {
        binding.clHomeSetRegion.setOnClickListener {
            toggleRegionListVisibility()
        }

        binding.tvHomeReSelectRegionBtn.setOnClickListener {
            reselectRegion()
        }

        val regionViews = listOf(
            binding.tvHomeSelectRegion1,
            binding.tvHomeSelectRegion2,
            binding.tvHomeSelectRegion3
        )

        regionViews.forEachIndexed { index, regionView ->
            regionView.setOnClickListener {
                selectRegion(regionView, index + 1, regionViews)
            }
        }

        binding.viewControlSpinner.setOnClickListener {
            toggleRegionListVisibility()
        }
    }

    // 알림 버튼 클릭 시 알림 화면으로 이동
    private fun setupNotificationClick() {
        binding.ivNotification.setOnClickListener {
            val notificationFragment = NotificationsFragment.newInstance()
            notificationFragment.show(
                requireActivity().supportFragmentManager, "NotificationFragment"
            )
            homeViewModel.hideNotificationSign()
        }
    }

    // 배너 클릭 시 랜덤 서비스 상세 페이지로 이동
    private fun setupBannerClick() {
        binding.ivHomeMainBanner.setOnClickListener {
            if (homeViewModel.randomService.isEmpty()) {
                Toast.makeText(requireContext(), "최근에 나온 서비스가 없습니다.", Toast.LENGTH_SHORT).show()
            } else {
                val dialog = DetailFragment.newInstance(homeViewModel.randomService.random())
                dialog.setCloseListener(object : DetailCloseInterface { // 다이얼로그 종료 리스너를 받아 onResume으로 갱신하기
                    override fun onDialogClosed() {
                        onResume()
                    }
                })
                dialog.show(requireActivity().supportFragmentManager, "Detail")
            }
        }
    }

    // 지역 선택 화면을 토글
    private fun toggleRegionListVisibility() {
        if (binding.clHomeRegionList.isVisible) {
            binding.ivHomeMoreBtn.setImageResource(R.drawable.ic_more)
            binding.viewControlSpinner.isVisible = false
        } else {
            binding.ivHomeMoreBtn.setImageResource(R.drawable.ic_less)
            binding.viewControlSpinner.isVisible = true
        }
        binding.clHomeRegionList.isVisible = !binding.clHomeRegionList.isVisible
    }

    // 지역을 다시 선택할 때, 지역 선택 화면으로 이동
    private fun reselectRegion() {
        binding.clHomeRegionList.isVisible = false
        binding.ivHomeMoreBtn.setImageResource(R.drawable.ic_more)
        binding.viewControlSpinner.isVisible = false
        val intent = Intent(context, InterestRegionSelectActivity::class.java)
        resultLauncher.launch(intent)
    }

    // 지역을 선택했을 때, 선택된 지역을 표시하고, 선택된 지역을 저장
    private fun selectRegion(regionView: TextView, index: Int, regionViews: List<TextView>) {
        regionViews.forEach { view ->
            if (view == regionView) {
                view.setTextColor(requireContext().getColor(R.color.total_text_color))
                mainViewModel.setRegion(regionView.text.toString())
            } else {
                view.setTextColor(requireContext().getColor(R.color.unable_button_text))
            }
        }

        binding.tvHomeCurrentRegion.text = regionView.text
        binding.clSearchResults.isVisible = false
        binding.etSearch.setText("")
        binding.etSearch.hint = "${regionView.text} 공공서비스 대상 검색"
        binding.clHomeRegionList.isVisible = false
        binding.ivHomeMoreBtn.setImageResource(R.drawable.ic_more)
        binding.viewControlSpinner.isVisible = false
        homeViewModel.saveSelectedRegion(index)
    }

    // 검색어 저장 목록을 숨김
    private fun hideSearchHistory() {
        // 포커스가 EditText에서 벗어났을 때 검색어 저장 목록을 표시하는 RecyclerView를 숨김
        binding.rvSearchHistory.visibility = View.GONE
    }

    // 키보드 숨기기
    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
    }

    // 검색 결과를 클릭했을 때 상세 페이지로 이동
    private fun searchClick(adapter: HomeSearchAdapter) {
        adapter.categoryItemClick = object : CategoryItemClick {
            override fun onClick(svcID: String) {
                val dialog = DetailFragment.newInstance(svcID)
                dialog.show(requireActivity().supportFragmentManager, "CategoryFrag")
            }
        }
    }

    // 각 지역에 있는 서비스의 개수를 보여주는 텍스트를 설정
    private fun setSpannableString(start: Int, end: Int): SpannableString {
        val spannableString = SpannableString(binding.tvHomeDescription.text)

        if (mainViewModel.selectRegion.value?.length!! > 3) {
            spannableString.setSpan(
                ForegroundColorSpan(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.total_text_color
                    )
                ),
                start + 1, end + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            spannableString.setSpan(
                StyleSpan(Typeface.BOLD),
                start + 1, end + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        } else {
            spannableString.setSpan(
                ForegroundColorSpan(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.total_text_color
                    )
                ),
                start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            spannableString.setSpan(
                StyleSpan(Typeface.BOLD),
                start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return spannableString
    }

    private fun setupRecentData() { // 최근 검색어 존재할 때 viewPager를 띄우는 부분
        if (homeViewModel.recentData.value.isNullOrEmpty()) {
            binding.vpHomeRecent.visibility = View.GONE
            binding.tvHomeRecentDescription.visibility = View.GONE
            binding.tvHomeRecentTitle.visibility = View.GONE
        } else {
            binding.vpHomeRecent.visibility = View.VISIBLE
            binding.tvHomeRecentDescription.visibility = View.VISIBLE
            binding.tvHomeRecentTitle.visibility = View.VISIBLE
        }
    }

    // 최근에 들어간 서비스를 보여주는 viewPager
    private fun recentViewPager(data: List<RecentEntity>) {
        val viewPage = binding.vpHomeRecent
        val adapter = RecentAdapter()
        viewPage.adapter = adapter
        viewPage.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        adapter.submitList(data)
        TabLayoutMediator(binding.vpHomeRecentIndicator, binding.vpHomeRecent) { _, _ -> }.attach() // Indicator 연결
        viewPage.offscreenPageLimit = 1
        adapter.itemClick = object : CategoryItemClick {
            override fun onClick(svcID: String) {
                val dialog = DetailFragment.newInstance(svcID)
                dialog.setCloseListener(object : DetailCloseInterface {
                    override fun onDialogClosed() {
                        onResume()
                    }
                })
                dialog.show(requireActivity().supportFragmentManager, "HomeRecent")
            }
        }
    }

    // 단기예보 지역 정보를 기상청 좌표로 변환한 후 API 요청
    private fun weatherDataSend(area: String) { // 단기예보
        CoroutineScope(Dispatchers.IO).launch {

                homeViewModel.fetchWeatherData()
                val seoul = WeatherSeoulArea().weatherSeoulArea
                if (WeatherData.getArea() == null || WeatherData.getArea()!! != area || WeatherData.getDate() != LocalDate.now().dayOfMonth) {
                    if (seoul.keys.contains(area)) {
                        WeatherData.saveAreaDate(area, LocalDate.now().dayOfMonth)
                        val seoulWeather = seoul[area]
                        Log.i(
                            "This is HomeFragment",
                            "seoulWeather : $seoulWeather\narea : $area\nfirst : ${seoulWeather?.first ?: "null"}\nsecond : ${seoulWeather?.second ?: "null"}"
                        )
                        homeViewModel.weatherShortData(
                            seoulWeather?.first ?: 60,
                            seoulWeather?.second ?: 127
                        )    // null일 경우 = 서울시청
                    }
                } else {
                    homeViewModel.weatherShortData(Int.MAX_VALUE, Int.MAX_VALUE)
                }
                Log.d("dkj2", "API 통신 실행")
        }
    }

    private fun weatherAdapter(short: List<WeatherShort>) { // 날씨 어댑터
        val adapter = WeatherAdapter()
        binding.rvHomeWeatherWeek.adapter = adapter
        binding.rvHomeWeatherWeek.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        Log.i("This is HomeFragment","short : $short")
        adapter.submitList(short)
    }

    override fun onResume() {
        super.onResume()
        homeViewModel.loadRecentData()
        binding.etSearch.setText("")
        binding.clSearchResults.isVisible = false
    }
}