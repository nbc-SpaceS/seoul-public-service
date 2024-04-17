package com.wannabeinseoul.seoulpublicservice.pref

import android.content.Context

/** 유저 아이디를 저장하는 Repository. */
interface IdPrefRepository {
    fun save(value: String)
    fun load(): String
}

class IdPrefRepositoryImpl(context: Context) : IdPrefRepository {

    private val pref = context.getSharedPreferences("IdPrefRepository", Context.MODE_PRIVATE)

    override fun save(value: String) {
        pref.edit().putString("userId", value).apply()
    }

    override fun load(): String = pref.getString("userId", "") ?: ""
}
