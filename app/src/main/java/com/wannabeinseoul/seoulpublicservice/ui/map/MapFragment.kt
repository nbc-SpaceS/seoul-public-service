package com.wannabeinseoul.seoulpublicservice.ui.map

import android.content.Context
import android.content.Intent
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
import com.naver.maps.map.util.MarkerIcons
import com.wannabeinseoul.seoulpublicservice.R
import com.wannabeinseoul.seoulpublicservice.SeoulPublicServiceApplication
import com.wannabeinseoul.seoulpublicservice.databinding.FragmentMapBinding
import com.wannabeinseoul.seoulpublicservice.ui.detail.DetailFragment
import com.wannabeinseoul.seoulpublicservice.ui.dialog.filter.FilterFragment
import com.wannabeinseoul.seoulpublicservice.ui.main.MainViewModel
import com.wannabeinseoul.seoulpublicservice.util.toastShort

class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var mapView: MapView
    private var naverMap: NaverMap? = null

    private val app by lazy {
        requireActivity().application as SeoulPublicServiceApplication
    }

    private val activeMarkers: MutableList<Marker> = mutableListOf()

    private val rvAdapter: MapOptionAdapter by lazy {
        MapOptionAdapter()
    }

    private val adapter: MapDetailInfoAdapter by lazy {
        MapDetailInfoAdapter(
            saveService = { id ->
                viewModel.saveService(id)
            },
            moveReservationPage = { url ->
                changeDetailVisible(false)
                zoomOut()

                activeMarkers.forEach { marker ->
                    marker.iconTintColor =
                        requireContext().getColor(matchingColor[marker.tag] ?: R.color.gray)
                    marker.zIndex = 0
                }

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
                changeDetailVisible(false)
                zoomOut()

                activeMarkers.forEach { marker ->
                    marker.iconTintColor =
                        requireContext().getColor(matchingColor[marker.tag] ?: R.color.gray)
                    marker.zIndex = 0
                }

                val dialog = DetailFragment.newInstance(id)
                dialog.show(requireActivity().supportFragmentManager, "Detail")
            },
            savedPrefRepository = viewModel.getSavedPrefRepository()
        )
    }

    private val viewModel: MapViewModel by viewModels { MapViewModel.factory }
    private val mainViewModel: MainViewModel by activityViewModels()

    private val matchingColor = hashMapOf(
        "체육시설" to R.color.marker1_solid,
        "교육강좌" to R.color.marker2_solid,
        "문화체험" to R.color.marker3_solid,
        "공간시설" to R.color.marker4_solid,
        "진료복지" to R.color.marker5_solid
    )

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

        mapView = binding.root.findViewById(R.id.mv_naver) as MapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        addCallBack()
        initViewModel()

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
    }

    private fun initView() {
        binding.vpMapDetailInfo.adapter = adapter
        binding.vpMapDetailInfo.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.tvMapInfoCount.text = "${position + 1}"
            }
        })
        binding.vpMapDetailInfo.offscreenPageLimit = 1

        binding.rvMapSelectedOption.adapter = rvAdapter
        rvAdapter.submitList(viewModel.loadSavedOptions().flatten())

        binding.tvMapFilterBtn.setOnClickListener {
            changeDetailVisible(false)

            activeMarkers.forEach { marker ->
                marker.iconTintColor =
                    requireContext().getColor(matchingColor[marker.tag] ?: R.color.gray)
                marker.zIndex = 0
            }

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
                    viewModel.setServiceData(textView.text.toString())
                } else {
                    viewModel.setServiceData()
                }
                setInitialState()
                moveCamera(null, null, 10.0)
                true
            }
            false
        }

        binding.etMapSearch.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                changeDetailVisible(false)
            }
        }

        viewModel.initMap()
        mainViewModel.applyFilter.observe(viewLifecycleOwner) { isApply ->
            if (isApply) {
                if (binding.etMapSearch.text.isNotEmpty()) {
                    viewModel.setServiceData(binding.etMapSearch.text.toString())
                } else {
                    viewModel.setServiceData()
                }
                rvAdapter.submitList(viewModel.loadSavedOptions().flatten())
                moveCamera(null, null, 10.0)
                if (viewModel.loadSavedOptions().any { it.isNotEmpty() }) {
                    binding.tvMapFilterBtn.setTextColor(requireContext().getColor(R.color.point_color))
                    binding.clMapFilterCount.isVisible = true
                    binding.tvMapFilterCount.text = viewModel.filterCount.toString()
                } else {
                    binding.tvMapFilterBtn.setTextColor(requireContext().getColor(R.color.total_text_color))
                    binding.clMapFilterCount.isVisible = false
                }
            }
        }
    }

    private fun initViewModel() = with(viewModel) {
        viewModel.setServiceData()

        updateData.observe(viewLifecycleOwner) { list ->
            adapter.submitList(emptyList())
            adapter.submitList(list.toList())
            binding.tvMapInfoCount.text = "1"
        }

        canStart.observe(viewLifecycleOwner) { start ->
            if (start) {
                activeMarkers.forEach {
                    it.map = null
                }
                activeMarkers.clear()

                if (filteringData.value?.size == 0) {
                    toastShort(requireContext(), "필터링 결과가 없습니다.")
                } else {
                    toastShort(requireContext(), "${filteringData.value?.size}+개의 서비스가 있습니다.")
                }

                filteringData.value?.forEach {
                    val marker = Marker()
                    activeMarkers.add(marker)
                    marker.position = LatLng(it.key.first.toDouble(), it.key.second.toDouble())
                    marker.map = naverMap
                    marker.icon = MarkerIcons.BLACK
                    marker.iconTintColor = requireContext().getColor(
                        matchingColor[it.value[0].MAXCLASSNM] ?: R.color.gray
                    )
                    marker.tag = it.value[0].MAXCLASSNM
                    if (it.value.size > 1) marker.captionText = it.value.size.toString()
                    marker.setCaptionAligns(Align.Top)
                    marker.captionTextSize = 16f
                    marker.captionMinZoom = 12.5
                    marker.captionMaxZoom = 18.0
                    marker.onClickListener = Overlay.OnClickListener { _ ->
                        changeDetailVisible(true)
                        activeMarkers.forEach { marker ->
                            marker.iconTintColor =
                                requireContext().getColor(matchingColor[marker.tag] ?: R.color.gray)
                            marker.zIndex = 0
                        }
                        marker.iconTintColor =
                            requireContext().getColor(R.color.point_color)
                        marker.zIndex = 10
                        viewModel.updateInfo(it.value)
                        binding.vpMapDetailInfo.setCurrentItem(0, false)
                        moveCamera(it.key.first.toDouble(), it.key.second.toDouble())
                        true
                    }
                }
            }
        }
    }

    override fun onMapReady(map: NaverMap) {

        naverMap = map
        map.maxZoom = 18.0
        map.minZoom = 9.0

        viewModel.checkReadyMap()

        map.setOnMapClickListener { _, _ ->
            changeDetailVisible(false)
            activeMarkers.forEach { marker ->
                marker.iconTintColor =
                    requireContext().getColor(matchingColor[marker.tag] ?: R.color.gray)
                marker.zIndex = 0
            }
            zoomOut()
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

    private val cityHall = LatLng(37.5666, 126.9782)
    private fun LatLng.inSeoulOrNull() =
        if (this.latitude in 37.413294..37.715133 &&
            this.longitude in 126.734086..127.269311
        ) this else null

    /** null이면 차례대로 파라미터 좌표 -> 현재위치(서울범위) -> 시청 */
    private fun moveCamera(latLng: LatLng?, zoom: Double = 15.0) {
        val pos = latLng ?: app.getLastLatLng()?.inSeoulOrNull() ?: cityHall
        val cameraUpdate = CameraUpdate.scrollAndZoomTo(pos, zoom)
            .animate(CameraAnimation.Easing, 600)
        naverMap?.moveCamera(cameraUpdate)
    }

    /** null이면 차례대로 파라미터 좌표 -> 현재위치(서울범위) -> 시청 */
    private fun moveCamera(y: Double?, x: Double?, zoom: Double = 15.0) {
        val latLng = if (y == null || x == null) null else LatLng(y, x)
        moveCamera(latLng, zoom)
    }

    private fun moveCameraToCityHall() = moveCamera(cityHall, 15.0)

    private fun changeDetailVisible(flag: Boolean) {
        binding.vpMapDetailInfo.isVisible = flag
        binding.clMapInfoCount.isVisible = flag
        binding.fabMapCurrentLocation.isVisible = !flag
    }

    private fun zoomOut() {
        val cameraUpdate = CameraUpdate.zoomTo(14.5).animate(CameraAnimation.Easing, 300)
        naverMap?.moveCamera(cameraUpdate)
    }

    private fun setInitialState() {
        binding.etMapSearch.clearFocus()

        val inputMethodManager =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(
            binding.etMapSearch.windowToken,
            0
        )
    }

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
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.etMapSearch.setText("")
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