package com.wannabeinseoul.seoulpublicservice.ui.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.wannabeinseoul.seoulpublicservice.ui.home.HomeFragment
import com.wannabeinseoul.seoulpublicservice.ui.map.MapFragment
import com.wannabeinseoul.seoulpublicservice.ui.mypage.MyPageFragment
import com.wannabeinseoul.seoulpublicservice.ui.recommendation.RecommendationFragment

class ViewPagerAdapter(fragmentActivity: FragmentActivity): FragmentStateAdapter(fragmentActivity) {

    private val fragments = listOf(
        HomeFragment(),
        MapFragment(),
        RecommendationFragment(),
        MyPageFragment.newInstance()
    )

    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun createFragment(position: Int): Fragment = fragments[position]

}