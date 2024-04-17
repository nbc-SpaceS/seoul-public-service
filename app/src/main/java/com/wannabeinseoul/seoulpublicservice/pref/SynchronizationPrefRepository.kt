package com.wannabeinseoul.seoulpublicservice.pref

import android.content.Context

/** 동기화 키를 저장하는 Repository. */
interface SynchronizationPrefRepository {
    fun save(value: String)
    fun load(): String
}

class SynchronizationPrefRepositoryImpl(context: Context) : SynchronizationPrefRepository {

    private val pref = context.getSharedPreferences("SynchronizationPrefRepository", Context.MODE_PRIVATE)

    override fun save(value: String) {
        pref.edit().putString("key", value).apply()
    }

    override fun load(): String = pref.getString("key", "") ?: ""
}
