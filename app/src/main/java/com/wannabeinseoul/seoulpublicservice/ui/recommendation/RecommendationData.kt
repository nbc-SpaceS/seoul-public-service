package com.wannabeinseoul.seoulpublicservice.ui.recommendation

import com.wannabeinseoul.seoulpublicservice.seoul.Row

data class RecommendationData(
    val payType: String,
    val areaName: String,
    val placeName: String,
    val svcstatnm: String,
    val imageUrl: String,
    val svcid: String,
    val usetgtinfo: String,
    var reviewCount: Int
)

fun Row.convertToRecommendationData() = RecommendationData(
    payType = this.payatnm,
    areaName = this.areanm,
    placeName = this.placenm,
    svcstatnm = this.svcstatnm,
    imageUrl = this.imgurl,
    svcid = this.svcid,
    usetgtinfo = this.usetgtinfo,
    reviewCount = 0
)
//플레이스네ㅐ임을 서비스스텟으로

fun List<Row>.convertToRecommendationDataList() =
    this.map { it.convertToRecommendationData() }


//    private fun convertRowToRecommendation(row: Row): RecommendationData = RecommendationData(
//        payType = row.payatnm,
//        areaName = row.areanm,
//        placeName = row.placenm,
//        svcstatnm = row.svcstatnm,
//        imageUrl = row.imgurl,
//    )
//
//    private fun convertRowsToRecommendations(rows: List<Row>): List<RecommendationData> =
//        rows.map { convertRowToRecommendation(it) }

