package com.wannabeinseoul.seoulpublicservice.pref

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson

interface SavedPrefRepository {
    fun getSvcidList(): List<String>
    fun setSvcidList(list: List<String>)
    fun clear()
    fun addSvcidList(svcidList: List<String>)
    fun addSvcid(svcid: String)
    fun remove(svcid: String)
    fun contains(svcid: String): Boolean
    val savedSvcidListLiveData: LiveData<List<String>>
}

class SavedPrefRepositoryImpl(context: Context) : SavedPrefRepository {

    private val pref = context.getSharedPreferences("SavedPrefRepository", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val keySvcidList = "keySvcidList"

    private val _savedSvcidList: MutableLiveData<List<String>> =
        MutableLiveData(getSvcidListFromPref())
    override val savedSvcidListLiveData: LiveData<List<String>> get() = _savedSvcidList
    private var beforeHashCode = savedSvcidListLiveData.value.hashCode()

    init {
        savedSvcidListLiveData.observeForever { list ->
            if (list.hashCode() == beforeHashCode) return@observeForever
            beforeHashCode = list.hashCode()

            setSvcidListToPref(list)
        }
    }

    private fun getSvcidListFromPref(): List<String> {
        val json = pref.getString(keySvcidList, null) ?: return emptyList<String>()
            .apply { Log.w("jj-SavedPrefRepositoryImpl", "getSvcidListFromPref got null") }
        Log.d("jj-SavedPrefRepositoryImpl", "getSvcidListFromPref json: ${json.take(255)}")
        return gson.fromJson(json, Array<String>::class.java).toList()
    }

    private fun setSvcidListToPref(list: List<String>) {
        val json = gson.toJson(list)
        pref.edit().putString(keySvcidList, json).apply()
        Log.d("jj-SavedPrefRepositoryImpl", "setSvcidListToPref json: ${json.take(255)}")
    }

    override fun getSvcidList(): List<String> = savedSvcidListLiveData.value!!

    override fun setSvcidList(list: List<String>) {
        _savedSvcidList.value = list
    }

    override fun clear() = setSvcidList(emptyList())

    override fun addSvcidList(svcidList: List<String>) = setSvcidList(getSvcidList() + svcidList)

    override fun addSvcid(svcid: String) = setSvcidList(getSvcidList() + svcid)

    override fun remove(svcid: String) = setSvcidList(getSvcidList() - svcid)

    override fun contains(svcid: String) = getSvcidList().contains(svcid)

}
