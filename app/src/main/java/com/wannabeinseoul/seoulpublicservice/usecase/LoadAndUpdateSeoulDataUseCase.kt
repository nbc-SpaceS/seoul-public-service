package com.wannabeinseoul.seoulpublicservice.usecase

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.wannabeinseoul.seoulpublicservice.databases.ReservationEntity
import com.wannabeinseoul.seoulpublicservice.databases.ReservationRepository
import com.wannabeinseoul.seoulpublicservice.db_by_memory.DbMemoryRepository
import com.wannabeinseoul.seoulpublicservice.pref.PrefRepository
import com.wannabeinseoul.seoulpublicservice.seoul.SeoulPublicRepository
import com.wannabeinseoul.seoulpublicservice.util.DLog
import com.wannabeinseoul.seoulpublicservice.util.toastLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.CoroutineContext

private const val JJTAG = "jj-LoadAndUpdateSeoulDataUseCase"

// TODO: pref 레포 하나 파야할듯
private const val KEY_SAVED_TIME = "KEY_SAVED_TIME"

class LoadAndUpdateSeoulDataUseCase(
    private val context: Context,
    private val seoulPublicRepository: SeoulPublicRepository,
    private val prefRepository: PrefRepository,
    private val dbMemoryRepository: DbMemoryRepository,
    private val reservationRepository: ReservationRepository,
) {
    // TODO: dbMemory post로 할 수 있을지 고민
    // 혹시 Default로 돌리는게 나을까 싶어서 비교 가능하도록 파라미터로 받게 만듦. 나중에 삭제할지도?
    /** 코루틴 비동기 실행 */
    operator fun invoke(
        coroutineContext: CoroutineContext = Dispatchers.IO,
        callback: () -> Unit,
    ) = CoroutineScope(coroutineContext).launch {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val cap = cm.getNetworkCapabilities(cm.activeNetwork)
        if (cap == null ||
            (cap.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).not()
        ) {
            Log.d(JJTAG, "네트워크 사용 불가능: $cap")
            CoroutineScope(Dispatchers.Main).launch {
                toastLong(context, "네트워크 연결이 불가능하므로 저장된 데이터로 표시됩니다.")
            }
            loadFromRoomToDbMemory()
            return@launch callback()
        }

        var isOld = true
        prefRepository.load(KEY_SAVED_TIME)?.also {
            val savedTime = it.toLongOrNull() ?: return@also Unit
                .apply { Log.w(JJTAG, "toLong failed: ${it.take(255)}") }
            val timeDiff = System.currentTimeMillis() - savedTime
            isOld = timeDiff > 10800_000L
            DLog.d(JJTAG, "timeDiff: $timeDiff")
        }

        if (isOld) {
            getAndUpdateAll()
        } else {
            val reservationEntities = loadFromRoomToDbMemory()
            if (reservationEntities.isEmpty()) getAndUpdateAll()
        }


        callback()
    }

    private suspend fun getAndUpdateAll() {
        try {
            withTimeout(10_000L) {
                val startTime = System.currentTimeMillis()
                val total = seoulPublicRepository.getTotalNum()
                val reservationEntities =
                    seoulPublicRepository.getAllParallelAsReservationEntities(total)
                DLog.v(
                    JJTAG,
                    "getAndUpdateAll ${reservationEntities.size}/$total: ${(System.currentTimeMillis() - startTime).toFloat() / 1000}"
                )
                withContext(Dispatchers.Main) {
                    dbMemoryRepository.setAll(reservationEntities)
                }
                if (total == reservationEntities.size) {
                    reservationRepository.deleteAll()
                    reservationRepository.insertAll(reservationEntities)
                    prefRepository.save(KEY_SAVED_TIME, System.currentTimeMillis().toString())
                }
            }
        } catch (e: Throwable) {
            Log.e(JJTAG, "getAndUpdateAll 실패 (주로 Timeout)", e)
            CoroutineScope(Dispatchers.Main).launch {
                toastLong(context, "데이터를 받아오는 과정에서 문제가 발생했습니다. 저장된 데이터로 표시됩니다.")
            }
            loadFromRoomToDbMemory()
        }
    }

    private suspend fun loadFromRoomToDbMemory(): List<ReservationEntity> {
        val reservationEntities = reservationRepository.getAll()
        withContext(Dispatchers.Main) {
            dbMemoryRepository.setAll(reservationEntities)
        }
        return reservationEntities
    }

}
