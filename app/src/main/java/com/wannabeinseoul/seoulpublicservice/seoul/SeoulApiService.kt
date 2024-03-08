package com.wannabeinseoul.seoulpublicservice.seoul

import com.wannabeinseoul.seoulpublicservice.BuildConfig
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface SeoulApiService {

    @GET("/${BuildConfig.SEOUL_KEY}/json/tvYeyakCOllect/1/1000/")
    suspend fun getAll1000(): Response<SeoulDto>

    @GET("/${BuildConfig.SEOUL_KEY}/json/tvYeyakCOllect/{startIndex}/{endIndex}/")
    suspend fun getAllRange(
        @Path("startIndex") startIndex: Int,
        @Path("endIndex") endIndex: Int
    ): Response<SeoulDto>

    @GET("/${BuildConfig.SEOUL_KEY}/json/ListPublicReservationDetail/1/1/{svcid}/")
    suspend fun getDetail(
        @Path("svcid") svcid: String
    ): Response<SeoulDetailDto>

}
