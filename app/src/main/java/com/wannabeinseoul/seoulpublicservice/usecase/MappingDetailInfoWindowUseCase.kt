package com.wannabeinseoul.seoulpublicservice.usecase

import com.wannabeinseoul.seoulpublicservice.databases.ReservationEntity
import com.wannabeinseoul.seoulpublicservice.pref.SavedPrefRepository
import com.wannabeinseoul.seoulpublicservice.ui.map.DetailInfoWindow

class MappingDetailInfoWindowUseCase(
    private val savedPrefRepository: SavedPrefRepository
) {
    val list = savedPrefRepository.getSvcidList()
    operator fun invoke(serviceInfoList: List<ReservationEntity>): List<DetailInfoWindow> = serviceInfoList.map {
        DetailInfoWindow(
            svcid = it.SVCID,
            imgurl = it.IMGURL,
            areanm = it.AREANM,
            svcnm = it.SVCNM,
            payatnm = it.PAYATNM,
            svcstatnm = it.SVCSTATNM,
            svcurl = it.SVCURL,
            saved = list.contains(it.SVCID)
        )
    }
}
