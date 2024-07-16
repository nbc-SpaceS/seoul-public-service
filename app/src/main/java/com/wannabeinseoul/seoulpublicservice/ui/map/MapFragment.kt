package com.wannabeinseoul.seoulpublicservice.ui.map

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.Align
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.Overlay
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.util.MarkerIcons
import com.wannabeinseoul.seoulpublicservice.R
import com.wannabeinseoul.seoulpublicservice.SeoulPublicServiceApplication
import com.wannabeinseoul.seoulpublicservice.databinding.FragmentMapBinding
import com.wannabeinseoul.seoulpublicservice.ui.detail.DetailCloseInterface
import com.wannabeinseoul.seoulpublicservice.ui.detail.DetailFragment
import com.wannabeinseoul.seoulpublicservice.ui.dialog.filter.FilterFragment
import com.wannabeinseoul.seoulpublicservice.ui.main.MainViewModel
import com.wannabeinseoul.seoulpublicservice.util.inSeoulOrNull
import com.wannabeinseoul.seoulpublicservice.util.toastShort
import kotlin.math.abs

class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var mapView: MapView
    private var naverMap: NaverMap? = null

    private val app by lazy {
        requireActivity().application as SeoulPublicServiceApplication
    }

    private val activeMarkers: MutableList<Marker> = mutableListOf()
    private var selectedMarker: Marker? = null

    private val rvAdapter: MapOptionAdapter by lazy {
        MapOptionAdapter()
    }

    private val adapter: MapDetailInfoAdapter by lazy {
        MapDetailInfoAdapter(
            saveService = { id ->
                viewModel.saveService(id)
            },
            moveReservationPage = { url ->
                backFromClickMarker()

                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(url)
                    )
                )
            },
            shareUrl = { url ->
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "text/html"
                intent.putExtra(Intent.EXTRA_TEXT, url)
                val text = "공유하기"

                startActivity(Intent.createChooser(intent, text))
            },
            moveDetailPage = { id ->
                backFromClickMarker()

                val dialog = DetailFragment.newInstance(id)
                dialog.setCloseListener(object : DetailCloseInterface {
                    override fun onDialogClosed() {
                        mainViewModel.setMappingData()
                    }
                })
                dialog.show(requireActivity().supportFragmentManager, "Detail")
            },
            backFromClickMarker = {
                backFromClickMarker()
            },
            savedPrefRepository = viewModel.getSavedPrefRepository()
        )
    }

    private val viewModel: MapViewModel by viewModels { MapViewModel.factory }
    private val mainViewModel: MainViewModel by activityViewModels { MainViewModel.factory }

    private val cityHall = LatLng(37.5666, 126.9782)

    private val matchingColor = hashMapOf(
        "체육시설" to R.color.marker1_solid,
        "교육강좌" to R.color.marker2_solid,
        "문화체험" to R.color.marker3_solid,
        "공간시설" to R.color.marker4_solid,
        "진료복지" to R.color.marker5_solid
    )

    private val strokePaint by lazy {
        Paint().apply {
            color = requireContext().getColor(
                R.color.gray
            )
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
    }

    private val fillPaint by lazy {
        Paint().apply {
            color = requireContext().getColor(
                R.color.black
            )
            isAntiAlias = true
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(
            inflater,
            container,
            false
        )

        mapView = binding.root.findViewById(R.id.mv_naver)!!
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        addCallBack()

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
        initViewModel()
        initMainViewModel()
    }

    private fun initView() {
        val transform = CompositePageTransformer()
        transform.addTransformer(MarginPageTransformer(8))
        transform.addTransformer { view: View, fl: Float ->
            val v = 1 - abs(fl)
            view.scaleY = 0.8f + v * 0.2f
        }

        binding.vpMapDetailInfo.adapter = adapter
        binding.vpMapDetailInfo.offscreenPageLimit = 3
        binding.vpMapDetailInfo.setPageTransformer(transform)
        binding.vpMapDetailInfo.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
//                binding.tvMapInfoCount.text = "${position + 1}"
            }
        })

        binding.rvMapSelectedOption.adapter = rvAdapter
        rvAdapter.submitList(viewModel.loadSavedOptions().flatten())

        binding.tvMapFilterBtn.setOnClickListener {
            backFromClickMarker()

            val dialog = FilterFragment.newInstance()
            dialog.show(requireActivity().supportFragmentManager, "FilterFragment")
        }

        binding.fabMapCurrentLocation.setOnClickListener {
            val latLng = app.getLastLatLng()
            when {
                latLng == null -> {
                    toastShort(requireContext(), "현재 위치가 확인되지 않아 시청으로 이동합니다")
                    moveCameraToCityHall()
                }

                latLng.inSeoulOrNull() == null -> {
                    toastShort(requireContext(), "현재 위치가 서울 외부이므로 시청으로 이동합니다")
                    moveCameraToCityHall()
                }

                else -> moveCamera(latLng)
            }
        }

        binding.etMapSearch.setOnEditorActionListener { textView, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                if (textView.text.isNotEmpty()) {
                    mainViewModel.setMappingData(textView.text.toString(), true)
                } else {
                    mainViewModel.setMappingData(true)
                }

                setInitialState()
                moveCamera(latLng = null, 10.0)

                true
            }

            false
        }

        binding.etMapSearch.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                changeDetailVisible(false)
                backFromClickMarker()
            }
        }
    }

    private fun initMainViewModel() = with(mainViewModel) {
        applyFilter.observe(viewLifecycleOwner) { isApply ->
            if (isApply) {
                if (binding.etMapSearch.text.isNotEmpty()) {
                    mainViewModel.setMappingData(binding.etMapSearch.text.toString(), true)
                } else {
                    mainViewModel.setMappingData(true)
                }

                if (viewModel.loadSavedOptions().any { it.isNotEmpty() }) {
                    binding.tvMapFilterBtn.setTextColor(requireContext().getColor(R.color.point_color))
                    binding.clMapFilterCount.isVisible = true
                    binding.tvMapFilterCount.text = viewModel.filterCount.toString()
                } else {
                    binding.tvMapFilterBtn.setTextColor(requireContext().getColor(R.color.total_text_color))
                    binding.clMapFilterCount.isVisible = false
                }

                rvAdapter.submitList(viewModel.loadSavedOptions().flatten())
                moveCamera(latLng = null, 10.0)
            }
        }

        mappingData.observe(viewLifecycleOwner) {
            viewModel.setServiceData(it)
        }
    }

    private fun initViewModel() = with(viewModel) {
        updateData.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list.toList())
//            binding.tvMapInfoCount.text = "1"
        }

        filteringData.observe(viewLifecycleOwner) { dataSet ->
            activeMarkers.forEach {
                it.map = null
            }

            activeMarkers.clear()

            if (mainViewModel.isSearch) {
                if (dataSet.size == 0) {
                    toastShort(requireContext(), "필터링 결과가 없습니다.")
                } else {
                    toastShort(requireContext(), "${filteringData.value?.size}+개의 서비스가 있습니다.")
                }
            }

            dataSet.forEach {
                val marker = Marker()
                marker.position = LatLng(it.key.first.toDouble(), it.key.second.toDouble())
                marker.map = naverMap
                marker.icon = if (naverMap!!.cameraPosition.zoom > 13.1) {
                    MarkerIcons.BLACK
                } else {
                    createDotIcon()
                }
                marker.iconTintColor = requireContext().getColor(
                    matchingColor[it.value[0].maxclassnm] ?: R.color.gray
                )
                marker.tag = it.value[0].maxclassnm
                if (it.value.size > 1) marker.captionText = it.value.size.toString()
                marker.setCaptionAligns(Align.Top)
                marker.captionTextSize = 16f
                marker.captionMinZoom = 13.1
                marker.captionMaxZoom = 18.0
                marker.onClickListener = Overlay.OnClickListener { _ ->
                    changeDetailVisible(true)
                    selectedMarker?.let { prev ->
                        prev.iconTintColor = requireContext().getColor(matchingColor[prev.tag] ?: R.color.gray)
                        prev.zIndex = 0
                    }
                    marker.icon = MarkerIcons.BLACK
                    marker.iconTintColor =
                        requireContext().getColor(R.color.point_color)
                    marker.zIndex = 10
                    selectedMarker = marker
                    viewModel.updateInfo(it.value)
                    binding.vpMapDetailInfo.setCurrentItem(0, false)
                    moveCamera(LatLng(it.key.first.toDouble(), it.key.second.toDouble()))
                    true
                }

                activeMarkers.add(marker)
            }
        }
    }

    override fun onMapReady(map: NaverMap) {
        naverMap = map
        map.maxZoom = 18.0
        map.minZoom = 9.0

        mainViewModel.setMappingData()

        map.setOnMapClickListener { _, _ ->
            backFromClickMarker()
        }

        map.addOnCameraChangeListener { _, _ ->
            updateMarker()
        }

        val fusedLocationSource = app.fusedLocationSource
        map.locationSource = fusedLocationSource
        if (fusedLocationSource == null) {
            Log.w("jj-MapFragment", "onMapReady fusedLocationSource is null")
        } else {
            if (fusedLocationSource.isActivated.not()) {
                Log.w(
                    "jj-MapFragment",
                    "onMapReady fusedLocationSource is not activated. now activate."
                )
                fusedLocationSource.activate {}
            }
            map.locationTrackingMode = LocationTrackingMode.NoFollow
        }

        map.uiSettings.isLogoClickEnabled = false
        map.uiSettings.isScaleBarEnabled = false
        map.uiSettings.isCompassEnabled = false
        map.uiSettings.isZoomControlEnabled = false
        map.uiSettings.setLogoMargin(0, 0, 0, 0)
        map.uiSettings.isRotateGesturesEnabled = false

        moveCamera(latLng = null, 14.5)
    }

    /** 줌레벨 조건에 맞춰 마커를 업데이트하는 함수 */
    private fun updateMarker() {
        val zoom = naverMap!!.cameraPosition.zoom

        if (zoom > 13.1) {
            activeMarkers.forEach {
                if (it != selectedMarker) {
                    it.icon = MarkerIcons.BLACK
                    it.iconTintColor = requireContext().getColor(
                        matchingColor[it.tag] ?: R.color.gray
                    )
                }
            }
        } else {
            activeMarkers.forEach {
                if (it != selectedMarker) {
                    it.icon = createDotIcon()
                }
            }
        }
    }

    /** 원형 점 아이콘을 만드는 함수 */
    private fun createDotIcon(): OverlayImage {
        val size = 30  // 점의 크기 (픽셀)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawCircle(size / 2f, size / 2f, size / 2f, fillPaint)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, strokePaint)

        return OverlayImage.fromBitmap(bitmap)
    }

    /** LatLng를 넣었을 때 조건에 맞춰 카메라를 움직이는 함수
     *
     * null이면 차례대로 파라미터 좌표 -> 현재위치(서울범위) -> 시청 */
    private fun moveCamera(latLng: LatLng?, zoom: Double = 15.0) {
        val pos = latLng ?: app.getLastLatLng()?.inSeoulOrNull() ?: cityHall
        val cameraUpdate = CameraUpdate.scrollAndZoomTo(pos, zoom)
            .animate(CameraAnimation.Easing, 600)

        naverMap?.moveCamera(cameraUpdate)
    }

    /** 카메라를 시청으로 옮기는 함수 */
    private fun moveCameraToCityHall() = moveCamera(cityHall, 15.0)

    /** 정보창 노출 여부를 세팅하는 함수 */
    private fun changeDetailVisible(flag: Boolean) {
        binding.vpMapDetailInfo.isVisible = flag
//        binding.clMapInfoCount.isVisible = flag
        binding.fabMapCurrentLocation.isVisible = !flag
    }

    /** 지도페이지 초기 상태로 돌리는 함수 */
    private fun setInitialState() {
        binding.etMapSearch.clearFocus()

        val inputMethodManager =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(
            binding.etMapSearch.windowToken,
            0
        )
    }

    /** 클릭한 마커를 다시 초기상태로 돌리는 함수 */
    private fun backFromClickMarker() {
        changeDetailVisible(false)

        val zoom = naverMap!!.cameraPosition.zoom

        selectedMarker?.let { marker ->
            marker.icon = if (zoom > 13.1) {
                MarkerIcons.BLACK
            } else {
                createDotIcon()
            }

            marker.iconTintColor =
                requireContext().getColor(matchingColor[marker.tag] ?: R.color.gray)
            marker.zIndex = 0
        }

        selectedMarker = null
    }

    /** 지도페이지에서의 뒤로가기 버튼 처리 함수 */
    private fun addCallBack() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (binding.etMapSearch.hasFocus()) {
                binding.etMapSearch.clearFocus()
            } else {
                isEnabled = false
                requireActivity().onBackPressed()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mainViewModel.setMappingData(binding.etMapSearch.text.toString(), true)
        selectedMarker = null
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        backFromClickMarker()
        mapView.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView.onDestroy()

        // onStop, onDestroy에서 연결된 위치 객체를 비활성화시켜버림... 다시 켜주면 되긴 하는데 로딩이 걸린다.
        app.fusedLocationSource?.activate {}
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}