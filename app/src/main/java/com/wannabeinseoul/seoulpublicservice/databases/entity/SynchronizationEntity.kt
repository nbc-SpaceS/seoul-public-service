package com.wannabeinseoul.seoulpublicservice.databases.entity

data class SynchronizationEntity(
    val id: String,
    val name: String,
    val savedServiceList: List<String>
)