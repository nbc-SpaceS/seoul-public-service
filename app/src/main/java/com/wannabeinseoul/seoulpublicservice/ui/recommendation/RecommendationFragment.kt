package com.wannabeinseoul.seoulpublicservice.ui.recommendation

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.wannabeinseoul.seoulpublicservice.R
import com.wannabeinseoul.seoulpublicservice.databinding.FragmentRecommendationBinding
import com.wannabeinseoul.seoulpublicservice.ui.detail.DetailCloseInterface
import com.wannabeinseoul.seoulpublicservice.ui.detail.DetailFragment
import com.wannabeinseoul.seoulpublicservice.ui.main.MainViewModel
import com.wannabeinseoul.seoulpublicservice.ui.recommendation.RecommendationViewModel.Companion.factory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RecommendationFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    private lateinit var binding: FragmentRecommendationBinding
    private val viewModel: RecommendationViewModel by viewModels { factory }
    private val mainViewModel: MainViewModel by activityViewModels { factory }

    private val showDetailFragment: (RecommendationData) -> Unit =
        { recommendationData: RecommendationData ->
            val dialog = DetailFragment.newInstance(recommendationData.svcid)
            dialog.setCloseListener(object : DetailCloseInterface {
                override fun onDialogClosed() {
                    mainViewModel.setMappingData()
                }
            })
            dialog.show(requireActivity().supportFragmentManager, "Detail")
        }

    private val recommendationAdapter = RecommendationAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentRecommendationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initViewModel()
        binding.slRefresh.setOnRefreshListener(this)
        refreshColor()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initView() = binding.let { b ->
        b.rvScroll.adapter = recommendationAdapter
        b.rvScroll.itemAnimator = null
        b.rvScroll.layoutManager = LinearLayoutManager(requireContext())
        b.clRecommendationLoadingLayer.setOnTouchListener { _, _ -> true }
        b.clRecommendationInvisibleLayer.setOnTouchListener { _, _ -> true }
    }

    private val tipsMap = mapOf(
        "서울시 관련 Tip!" to listOf(
            "추천 서비스에서는 지역 설정에 따라 추천항목이 달라집니다.",
            "서울시에서 제공하는 공공서비스는 총 1700개 이상입니다.",
            "서울시의 주거 환경 개선 프로그램을 신청할 때, 자격 조건과 신청 절차를 충분히 숙지하고 신청하세요.",
            "서울시의 긴급 구호 및 안전 시설을 이용할 때, 비상 상황에 대비하여 위치를 파악하고 신속하게 대응하는 것이 중요합니다.",
            "서울시의 문화 예술 공연을 즐길 때는 미리 예매를 하여 혼잡을 피하세요.",
            "서울시의 공원과 산책로는 자전거를 타거나 걷기에 좋은 장소입니다.",
            "서울시 지하철을 이용할 때는 출퇴근 시간을 피하여 혼잡을 피하세요.",
            "서울시의 역사적인 명소와 문화유적지를 방문하여 다양한 경험을 즐겨보세요.",
            "서울시에서는 다양한 문화 체험 프로그램을 제공하니 참여해보세요.",
            "서울시의 다양한 음식 문화를 경험하기 위해 지역별 맛집을 탐방해보세요.",
            "서울시의 상징하는 마스코트는 해태(해치)입니다.",
            "서울시에 김포공항을 서울김포공항으로 바뀔 예정입니다.",
            "서울시의 신호등이 서울의 상징인 해치 캐릭터를 넣는 방안을 추진 중 입니다.",
            "시속 100km 이상의 속도로 수도권에서 서울 도심까지 출퇴근을 30분 만에 할 수 있는 'GTX-A'가 개통될 예정입니다.",
            "6월부터는 서울 한강에 '서울의 달'이라는 열기구가 건물 50층의 높이로 뜰 예정입니다.",
        ),
        "앱 관련 문제 Tip!" to listOf(
            "문제가 생겼을 때는 앱을 다시 시작해 보거나, 안전한 사용을 위해 앱을 업데이트해 보세요.",
            "앱에서 접속 문제가 발생할 경우, 저장된 캐시를 지우고 다시 시도해 보세요.",
            "앱이 느리게 작동할 때는 기기의 저장 용량을 확인하고 불필요한 파일을 정리해 보세요.",
            "문제 해결을 위해 앱을 삭제한 후 재설치해 보세요. 이 과정은 종종 문제를 해결하는 데 도움이 됩니다.",
            "앱이 정상적으로 작동하지 않을 때는 사용 중인 기기의 운영 체제를 최신 버전으로 업데이트해 보세요.",
            "앱에서 자주 발생하는 문제에 대한 해결책을 앱의 공식 웹사이트나 지원 페이지에서 찾아보세요.",
            "앱에서 화면이 깨질 때는 디스플레이 설정을 확인하고 해상도를 조정해 보세요.",
            "앱이 네트워크 연결 오류를 표시할 때는 Wi-Fi 연결을 확인하고 모바일 데이터를 사용해 보세요.",
            "앱이 자주 다운되는 경우에는 배터리 절약 모드를 비활성화하고 백그라운드에서 실행 중인 앱을 확인해 보세요.",
            "앱이 느려지거나 응답하지 않을 때는 기기를 다시 시작하고 메모리를 확보해 보세요.",
            "앱에서 푸시 알림이 도착하지 않을 때는 알림 설정을 확인하고 푸시 알림을 활성화해 보세요.",
            "앱이 반응하지 않을 때는 잠시 기다린 후 다시 시도해 보세요. 가끔은 일시적인 서버 문제 때문일 수도 있습니다.",
            "앱이 충돌할 때는 기기의 시스템 설정에서 앱 데이터와 캐시를 지우고 다시 시작해 보세요.",
            "앱에서 비정상적인 동작이 계속되면 해당 문제를 개발자에게 보고하여 빠른 해결을 요청해 보세요.",
        ),
        "생활 관련 Tip!" to listOf(
            "3월이 제철인 미나리는 끓는 소금물에 데쳐서 먹으면, 암 예방에 도움이 되는 성분이 증가합니다.초록빛이 선명한 미나리를 골라드세요.",
            "라면을 먹은 뒤 우유를 마시면 얼굴이 덜 붓습니다.라면의 나트륨이 얼굴을 붓게 하는데 우유의 칼륨 성분이 나트륨을 빨리 배출시킵니다.",
            "축구화는 물세탁 대신, 천이나 솔로 닦은 뒤 바람이 잘 통하는 그늘에서 말려두세요.밑창, 끈은 중성세제로 손세탁하세요.",
            "이른 봄에 캔 어린 쑥이 맛과 향이 좋습니다. 잎이 부드럽고 색깔이 연한 쑥을 고르세요.삶아서 냉동 보관하면 1년 내내 먹을 수 있습니다.",
            "오래 보관해야 할 식품은 냉장고 안쪽에, 빨리 먹어야 하는 음식은 문 쪽에 보관해두세요.문 쪽은 온도 변화가 크기 때문입니다.",
            "냉장고에 있던 달걀을 끓는 물에 바로 넣으면 온도 차 때문에 껍데기가 쉽게 금이 갑니다. 잠시 실온에 두었다가 삶아보세요.",
            "빨래할 때 많이 쓰는 과탄산소다는 살균· 표백 효과가 뛰어나 색깔 있는 옷을 빨 때 쓰면 탈색될 수 있습니다.흰색 옷 빨 때만 쓰세요.",
            "더덕은 뿌리가 희고 굵으며 곧게 쭉 뻗은 것이 싱싱하고 맛있습니다. 또 향이 진한 더덕이 좋습니다. 표면의 주름이 깊은 더덕은 피하세요.",
            "벽에 붙인 흡착판이 자꾸 떨어진다면 흡착 부분에 로션이나 샴푸를 발라보세요. 벽과 흡착판 사이 미세한 틈을 메워 흡착력이 살아납니다.",
            "수육을 삶을 때 김 빠진 콜라를 넣으면 잡내를 잡는 데 도움이 됩니다. 따로 설탕을 안 넣어도 단맛이 나고, 음식 색도 더 좋습니다.",
            "오래 쓴 칫솔은 종량제 봉투에 담아 버려야 합니다 . 칫솔은 손잡이, 칫솔모 등의 소재가 각각 달라 재활용이 어렵기 때문입니다.",
            "토마토는 실온 보관하는 게 좋습니다. 냉장 보관하면 당도가 떨어지고 물러지기 쉽습니다. 영양소도 파괴됩니다. 적당량만 사서 빨리 드세요.",
            "고기를 구워 먹고 나온 기름을 싱크대에 버리면 기름이 굳어 하수구가 막히기 쉽습니다. 기름을 호일에 담은 뒤 굳으면 쓰레기통에 버리세요.",
            "욕실 세면대 수도꼭지(수전)에 바셀린을 바르고 수건으로 닦아보세요. 광택이 살아나고 얼룩이 덜 생깁니다.",
            "요리할 때 쓰고 남은 콩나물은 다듬어 밀폐 용기에 넣고 물을 채워 냉장해 보세요. 신선하게 더 오래 보관할 수 있습니다.",
            "딸기는 소금을 약간 푼 물에 담갔다가 헹구어 먹어보세요. 소금이 딸기의 단맛을 살려줍니다. 잔류 농약 제거에도 효과가 있습니다.",
            "견과류는 상온에 두면 독소가 생기기 쉽습니다. 냉장하지 않은 견과류는 과감하게 버리는 게 낫습니다.",
            "남은 떡국떡을 물에 잠시 불린 뒤 180도로 설정한 에어프라이어에 10분 정도 조리하면 바삭한 간식이 됩니다.",
        ),
    )
    private val randomTipHeader: String = tipsMap.keys.random()
    private val randomTip: String = tipsMap[randomTipHeader]?.random() ?: ""

    private fun initViewModel() = viewModel.let { vm ->

        vm.horizontalDataList.observe(viewLifecycleOwner) { horizontalDataList ->
            val multiViews: MutableList<RecommendationAdapter.MultiView> = horizontalDataList.map {
                RecommendationAdapter.MultiView.Horizontal(
                    it.keyword,
                    it.title,
                    RecommendationHorizontalAdapter(showDetailFragment).apply { submitList(it.list) },
                    infiniteScrollLambdaFunc = { query, num ->
                        CoroutineScope(Dispatchers.IO).launch {
                            vm.getAdditionalQuery(query, num)
                        }
                    }
                )
            }.toMutableList()
            if (multiViews.size >= 1) {
                multiViews.add(1, RecommendationAdapter.MultiView.Tip(randomTipHeader, randomTip))
            }
            viewModel.setMultiViews(multiViews)
        }

        vm.multiViews.observe(viewLifecycleOwner) {
            recommendationAdapter.submitList(it) {
                binding.clRecommendationLoadingLayer.isVisible = false
                binding.clRecommendationInvisibleLayer.isVisible = false
            }
        }

        viewModel.refreshLoading.observe(viewLifecycleOwner) { refreshLoading ->
            // 새로고침 로딩 상태에 따라 애니메이션 표시 여부 설정
            if (refreshLoading) {
                // 로딩 중일 때 로딩 인디케이터를 보여줌
                showLoadingIndicator()
            } else {
                // 로딩이 완료되면 로딩 인디케이터를 감춤
                hideLoadingIndicator()
            }
        }
    }

    private fun showLoadingIndicator() {
        // 로딩 인디케이터를 보여줌
        binding.slRefresh.isRefreshing = true
    }

    private fun hideLoadingIndicator() {
        // 로딩 인디케이터를 감춤
        binding.slRefresh.isRefreshing = false
    }

    private fun refreshColor() {
        binding.slRefresh.setOnRefreshListener(this)
        val colors = resources.getIntArray(R.array.google_colors)
        binding.slRefresh.setColorSchemeColors(*colors)
    }

    override fun onRefresh() {
        binding.clRecommendationInvisibleLayer.isVisible = true
        // SwipeRefreshLayout로부터의 새로고침 요청 처리
        viewModel.refreshData()
    }

    override fun onResume() {
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.fetchRegionList()
        }
        super.onResume()
    }
}