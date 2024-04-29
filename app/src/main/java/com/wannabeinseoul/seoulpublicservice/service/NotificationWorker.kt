package com.wannabeinseoul.seoulpublicservice.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.gson.Gson
import com.wannabeinseoul.seoulpublicservice.SeoulPublicServiceApplication
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class NotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val container = (applicationContext as SeoulPublicServiceApplication).container
            val savedList = getSvcIdList()
            val database = container.dbMemoryRepository
            val savedServiceList = savedList.map { database.findBySvcid(it) }

            val datePattern = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val datePattern2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH")
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S")

            // 예약 시작까지 하루 남은 서비스의 개수
            val list = savedServiceList.filter {
                datePattern2.format(
                    LocalDateTime.parse(
                        it?.RCPTBGNDT,
                        formatter
                    )
                ) >= datePattern2.format(
                    LocalDateTime.now().plusDays(1)
                ) && datePattern.format(
                    LocalDateTime.parse(
                        it?.RCPTBGNDT,
                        formatter
                    )
                ) == datePattern.format(
                    LocalDateTime.now().plusDays(1)
                )
            }.size

            // 예약 마감까지 하루 남은 서비스의 개수
            val list2 = savedServiceList.filter {
                datePattern2.format(
                    LocalDateTime.parse(
                        it?.RCPTENDDT,
                        formatter
                    )
                ) >= datePattern2.format(
                    LocalDateTime.now().plusDays(1)
                ) && datePattern.format(
                    LocalDateTime.parse(
                        it?.RCPTENDDT,
                        formatter
                    )
                ) == datePattern.format(
                    LocalDateTime.now().plusDays(1)
                )
            }.size

            // 예약 시작한 서비스의 개수
            val list3 = savedServiceList.filter {
                datePattern.format(
                    LocalDateTime.parse(
                        it?.RCPTBGNDT,
                        formatter
                    )
                ) == datePattern.format(
                    LocalDateTime.now()
                )
            }.size

            val outputData: Data = Data.Builder()
                .putBoolean("result", (list != 0 || list2 != 0 || list3 != 0))
                .build()

            setProgress(outputData)
            delay(1000)
            Result.success()
        } catch (throwable: Throwable) {
            Result.failure()
        }
    }

    private val pref =
        appContext.getSharedPreferences("SavedPrefRepository", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val keySvcidList = "keySvcidList"
    private fun getSvcIdList(): List<String> {
        val json = pref.getString(keySvcidList, null) ?: return emptyList()
        return gson.fromJson(json, Array<String>::class.java).toList()
    }
}