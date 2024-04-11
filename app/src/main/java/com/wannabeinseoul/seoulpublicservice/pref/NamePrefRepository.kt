package com.wannabeinseoul.seoulpublicservice.pref

import android.content.Context

/** 유저 이름을 저장하는 Repository. */
interface NamePrefRepository {
    fun save(value: String)
    fun load(): String
}

class NamePrefRepositoryImpl(context: Context) : NamePrefRepository {

    private val pref = context.getSharedPreferences("NamePrefRepository", Context.MODE_PRIVATE)

    override fun save(value: String) {
        pref.edit().putString("userName", value).apply()
    }

    override fun load(): String = pref.getString("userName", "") ?: ""
}
