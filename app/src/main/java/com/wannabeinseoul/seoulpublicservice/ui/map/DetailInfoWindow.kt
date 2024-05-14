package com.wannabeinseoul.seoulpublicservice.ui.map

data class DetailInfoWindow(
    val maxclassnm: String,
    val svcid: String,
    val imgurl: String,
    val areanm: String,
    val svcnm: String,
    val payatnm: String,
    val svcstatnm: String,
    val svcurl: String,
    val saved: Boolean = false
)
