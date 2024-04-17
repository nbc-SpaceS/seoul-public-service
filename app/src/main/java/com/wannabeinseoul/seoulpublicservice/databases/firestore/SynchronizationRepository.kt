package com.wannabeinseoul.seoulpublicservice.databases.firestore

import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.wannabeinseoul.seoulpublicservice.databases.entity.SynchronizationEntity
import kotlinx.coroutines.tasks.await

interface SynchronizationRepository {

    suspend fun upload(id: String, name: String, savedServiceList: List<String>): String
    suspend fun revise(key: String, id: String, name: String, savedServiceList: List<String>): String
    suspend fun download(key: String): SynchronizationEntity
}

class SynchronizationRepositoryImpl: SynchronizationRepository {

    private val fireStore = Firebase.firestore

    override suspend fun upload(id: String, name: String, savedServiceList: List<String>): String {
        return try {
            val result = fireStore.collection("synchronization").add(SynchronizationEntity(id, name, savedServiceList)).await()
            result.id
        } catch (e: Exception) {
            ""
        }
    }

    override suspend fun revise(key: String, id: String, name: String, savedServiceList: List<String>): String {
        return try {
            fireStore.collection("synchronization").document(key).set(SynchronizationEntity(id, name, savedServiceList)).await()
            key
        } catch (e: Exception) {
            ""
        }
    }

    override suspend fun download(key: String): SynchronizationEntity {
        return try {
            val result = fireStore.collection("synchronization").document(key).get().await().data
            Log.d("dkj", "$result")
            SynchronizationEntity(
                result?.get("id") as? String ?: "",
                result?.get("name") as? String ?: "",
                result?.get("savedServiceList") as? List<String> ?: emptyList()
            )
        } catch (e: Exception) {
            Log.d("dkj", "$e")
            SynchronizationEntity.empty()
        }
    }
}