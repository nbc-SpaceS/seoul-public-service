package com.wannabeinseoul.seoulpublicservice.ui.detail

import android.Manifest
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.util.MarkerIcons
import com.wannabeinseoul.seoulpublicservice.R
import com.wannabeinseoul.seoulpublicservice.SeoulPublicServiceApplication
import com.wannabeinseoul.seoulpublicservice.databases.RecentEntity
import com.wannabeinseoul.seoulpublicservice.databases.ReservationEntity
import com.wannabeinseoul.seoulpublicservice.databinding.FragmentDetailBinding
import com.wannabeinseoul.seoulpublicservice.ui.main.MainViewModel
import com.wannabeinseoul.seoulpublicservice.util.loadWithHolder
import java.time.LocalDateTime

private const val DETAIL_PARAM = "detail_param1"

class DetailFragment : DialogFragment(), OnMapReadyCallback {
    private lateinit var mapView: MapView
    private lateinit var naverMap: NaverMap

    private var _binding: FragmentDetailBinding? = null
    val binding get() = _binding!!

    private val app by lazy { requireActivity().application as SeoulPublicServiceApplication }
    private val viewModel: DetailViewModel by viewModels { DetailViewModel.factory }
    private val mainViewModel: MainViewModel by activityViewModels()

    private var param1: String? = null
    private var textOpen = false    // 텍스트 뷰가 펼쳐져 있는지(false = 접힌 상태, true = 펼친 상태)

    private lateinit var itemLocation: LatLng // 아이템 위치

    private var closeListener: DetailCloseInterface? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(DETAIL_PARAM)
        }
        viewModel.getData(param1!!)
        viewModel.savedID(param1!!)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) =
        Dialog(requireContext(), R.style.DetailTransparent)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        mapView = binding.root.findViewById(R.id.mv_detail_maps) as MapView
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView.onCreate(savedInstanceState)
        favorite(viewModel.savedID.value!!)
        mapViewSet()
        viewModelInit()
        viewInit()
        mapView.getMapAsync(this)
    }

    private fun saveRecent(data: ReservationEntity) {
        val recentItem = RecentEntity(
            DATETIME = viewModel.dateFormatRecent(LocalDateTime.now()),
            SVCID = data.SVCID,
            AREANM = data.AREANM,
            IMGURL = data.IMGURL,
            MINCLASSNM = data.MINCLASSNM,
            SVCNM = data.SVCNM,
            SVCSTATNM = data.SVCSTATNM,
            PAYATNM = data.PAYATNM
        )
        viewModel.saveData(recentItem)
        Log.i("This is DetailFragment", "recent item : $recentItem")
    }

    private fun mapViewSet() {
        binding.mvDetailMaps.visibility = View.VISIBLE
        binding.ivDetailMapsSnapshot.visibility = View.INVISIBLE
    }

    private fun viewInit() = binding.let {
        it.btnDetailBack.setOnClickListener { viewModel.close(true) }
        it.tvDetailShowMore.setOnClickListener {
            viewModel.textOpened(!textOpen)
            showMore(textOpen)
        }
        it.ivDetailFavorite.setOnClickListener { viewModel.changeFavorite(param1!!) }
        it.btnDetailCall.setOnClickListener { call ->
            val pattern = """(\d{2,3}-\d{3,4}-\d{4})""".toRegex()
            val matchResult =
                viewModel.serviceData.value?.TELNO?.let { tel -> pattern.find(tel) }?.value
            if (matchResult.isNullOrBlank()) {
                call.isEnabled = false
            } else {
                call.isEnabled = true
                startActivity(
                    Intent(
                        Intent.ACTION_DIAL,
                        Uri.parse("tel:${matchResult}")
                    )
                )
            }
        }
        it.btnDetailReservation.setOnClickListener {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(viewModel.serviceData.value?.SVCURL)
                )
            )
        }
        it.ivDetailShare.setOnClickListener {
            val i = Intent(Intent.ACTION_SEND)
            i.type = "text/html"
            i.putExtra(Intent.EXTRA_TEXT, viewModel.serviceData.value!!.SVCURL)
            startActivity(Intent.createChooser(i, "링크 공유"))
        }

        keyEvent()
    }

    private fun viewModelInit() = viewModel.let { vm ->
        vm.serviceData.observe(viewLifecycleOwner) { data ->
            checkLatLng(data)   // itemLocation은 여기서 검사해서 반환함
            bind(data)
            saveRecent(data)
        }
        vm.textState.observe(viewLifecycleOwner) {
            textOpen = it
            showMore(it)
        }
        vm.closeEvent.observe(viewLifecycleOwner) { close -> if (close) dismiss() }
        vm.favoriteChanged.observe(viewLifecycleOwner) { favorite(it) }
        vm.distanceText.observe(viewLifecycleOwner) { binding.tvDetailDistanceFromHere.text = it }
    }

    private fun bind(data: ReservationEntity) {
        buttonDesign(data)
        binding.let {
            it.ivDetailImg.loadWithHolder(data.IMGURL)
            it.tvDetailTypeSmall.text = data.MINCLASSNM
            it.tvDetailName.text = Html.fromHtml(data.SVCNM, Html.FROM_HTML_MODE_LEGACY)
            it.tvDetailLocation.text =
                "${data.AREANM} - ${Html.fromHtml(data.PLACENM, Html.FROM_HTML_MODE_LEGACY)}"
            it.tvDetailDistanceFromHere.text = "현위치로부터 ?km"
            it.tvDetailUsetgtinfo.text = data.USETGTINFO.trim()
            it.tvDetailSvcopndt.text =
                "${viewModel.dateFormat(data.SVCOPNBGNDT)} ~ ${viewModel.dateFormat(data.SVCOPNENDDT)}"
            it.tvDetailRcptdt.text =
                "${viewModel.dateFormat(data.RCPTBGNDT)} ~ ${viewModel.dateFormat(data.RCPTENDDT)}"
            it.tvDetailV.text = "${data.V_MIN} ~ ${data.V_MAX}"
            it.tvDetailRevstdday.text = "${data.REVSTDDAYNM} ${data.REVSTDDAY}일 전"
            it.tvDetailDescription.text = Html.fromHtml(data.DTLCONT, Html.FROM_HTML_MODE_LEGACY)
        }
    }

    private fun checkLatLng(data: ReservationEntity): LatLng {  // 마커의 위치 처리
        return try {
            val x = data.X.toDoubleOrNull()
            val y = data.Y.toDoubleOrNull()
            itemLocation = if (x != null && y != null) {
                LatLng(
                    y.toDouble(),
                    x.toDouble()
                )   // latitude - 위도(-90 ~ 90) / longitude(-180 ~ 180) - 경도 : 검색할 때 위경도 순으로 검색해야 함
            } else {
                LatLng(0.0, 0.0)
            }

            distanceCheck()  // 어디에 둬야 할지 모르겠어서 여기에 둠

            itemLocation
        } catch (npe: NullPointerException) {
            Log.e("DetailFragment", "Error! : checkLatLng", npe)
            LatLng(0.0, 0.0)
        }
    }

    private fun distanceCheck() {
        val location = app.fusedLocationSource?.lastLocation
        val latLng = if (location == null) {
            LatLng(0.0, 0.0)
        } else {
            LatLng(location.latitude, location.longitude)
        }
        viewModel.distanceCheckResponse(viewModel.distance(itemLocation, latLng))
    }

    override fun onMapReady(nMap: NaverMap) {
        naverMap = nMap
        naverMap.apply {
            locationSource = app.fusedLocationSource
            if (app.fusedLocationSource?.isActivated == true &&
                ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // GPS 권한 없으면 이거 하면 멈춰버려서 권한 체크로 감싸줌
                locationTrackingMode = LocationTrackingMode.NoFollow
            }
            cameraPosition = CameraPosition(itemLocation, 14.0)
            uiSettings.apply {
                isLogoClickEnabled = false
                isScaleBarEnabled = false
                isCompassEnabled = false
                isZoomControlEnabled = false
                isScrollGesturesEnabled = false
                isScaleBarEnabled = false
                isRotateGesturesEnabled = false
                isZoomGesturesEnabled = false
                isIndoorLevelPickerEnabled = false
                isLocationButtonEnabled = false
                isTiltGesturesEnabled = false
                setLogoMargin(0, 0, 0, 0)
            }
            markerStyle()
        }
    }

    private fun markerStyle() {
        val marker = Marker()
        marker.position = itemLocation
        marker.map = naverMap
        marker.icon = MarkerIcons.BLACK
        marker.iconTintColor = requireContext().getColor(R.color.point_color)
        marker.width = 80
        marker.height = 100
    }

    private fun snapshotCallback() {
        naverMap.takeSnapshot {
            binding.ivDetailMapsSnapshot.setImageBitmap(it)
            binding.ivDetailMapsSnapshot.visibility = View.VISIBLE
            binding.mvDetailMaps.visibility = View.GONE
        }
    }

    private fun keyEvent() {
        dialog?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                snapshotCallback()
                Handler(Looper.getMainLooper()).postDelayed({
                    dismiss()
                }, 50)
                true
            } else {
                false
            }
        }
    }

    fun setCloseListener(listener: DetailCloseInterface) {
        closeListener = listener
    }

    override fun onDismiss(dialog: DialogInterface) {
        viewModel.close(false)
        closeListener?.onDialogClosed() // 다이얼로그 닫을 때 인터페이스 메서드 호출
        super.onDismiss(dialog)
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
        mapView.onDestroy()
        _binding = null
        super.onDestroyView()
        app.fusedLocationSource?.activate {}
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    private fun showMore(state: Boolean) {
        val text = binding.tvDetailDescription
        val more = binding.tvDetailShowMore
        val layoutParams = text.layoutParams
        more.let {
            when (state) {
                true -> {   // 펼쳐진 상태일 때
                    text.maxLines = Int.MAX_VALUE
                    layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    more.text = "접기..."
                }

                false -> {  // 접혀있는 상태일 때
                    text.maxLines = 6
                    layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    more.text = "더보기..."
                }
            }
            text.layoutParams = layoutParams
        }
    }

    private fun favorite(state: Boolean) {
        when (state) {
            true -> {
                binding.ivDetailFavorite.setImageResource(R.drawable.ic_save_fill)
                binding.ivDetailFavorite.drawable.setTint(requireContext().getColor(R.color.point_color))
            }

            false -> {
                binding.ivDetailFavorite.setImageResource(R.drawable.ic_save_empty)
            }
        }
    }

    private fun buttonDesign(data: ReservationEntity) {
        val button = binding.btnDetailReservation
        when (data.SVCSTATNM) {
            "접수중" -> {
                button.text = "예약하기"
                button.isEnabled = true
            }

            "안내중" -> {
                button.text = "예약안내"
                button.isEnabled = true
            }

            "접수종료" -> {
                button.text = "접수종료"
                button.isEnabled = false
            }

            "예약일시중지" -> {
                button.text = "예약일시중지"
                button.isEnabled = false
            }

            "예약마감" -> {
                button.text = "예약마감"
                button.isEnabled = false
            }
        }
        val telBtn = binding.btnDetailCall
        when {
            data.TELNO.isBlank() -> telBtn.isEnabled = false
            data.TELNO.isNotBlank() -> telBtn.isEnabled = true
        }
        val payment = binding.tvDetailPrice
        payment.text = data.PAYATNM.take(2)
        if (data.PAYATNM.take(2) == "유료") {
            payment.setTextColor(Color.parseColor("#000000"))
            payment.setBackgroundResource(R.drawable.background_badge_pay_type)
        } else {
            payment.setTextColor(Color.parseColor("#FFFFFF"))
            payment.setBackgroundResource(R.drawable.background_pointcolor_with_rounded)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(serviceID: String) =
            DetailFragment().apply {
                arguments = Bundle().apply {
                    putString(DETAIL_PARAM, serviceID)
                }
            }
    }
}