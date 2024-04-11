package com.wannabeinseoul.seoulpublicservice.ui.dialog.setting

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wannabeinseoul.seoulpublicservice.SeoulPublicServiceApplication
import com.wannabeinseoul.seoulpublicservice.databases.entity.SynchronizationEntity
import com.wannabeinseoul.seoulpublicservice.databases.firestore.SynchronizationRepository
import com.wannabeinseoul.seoulpublicservice.pref.IdPrefRepository
import com.wannabeinseoul.seoulpublicservice.pref.NamePrefRepository
import com.wannabeinseoul.seoulpublicservice.pref.SavedPrefRepository
import com.wannabeinseoul.seoulpublicservice.pref.SynchronizationPrefRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingViewModel(
    private val idPrefRepository: IdPrefRepository,
    private val savedPrefRepository: SavedPrefRepository,
    private val synchronizationRepository: SynchronizationRepository,
    private val synchronizationPrefRepository: SynchronizationPrefRepository,
    private val namePrefRepository: NamePrefRepository
) : ViewModel() {

    private val _synchronizationKey: MutableLiveData<String> = MutableLiveData()
    val synchronizationKey: LiveData<String> get() = _synchronizationKey

    private val _synchronizationData: MutableLiveData<SynchronizationEntity> = MutableLiveData()
    val synchronizationData: LiveData<SynchronizationEntity> get() = _synchronizationData

    fun storeDataToServer() {
        viewModelScope.launch(Dispatchers.IO) {
            val key = synchronizationPrefRepository.load()
            if (key.isNotEmpty()) {
                _synchronizationKey.postValue(
                    synchronizationRepository.revise(
                        key,
                        idPrefRepository.load(),
                        namePrefRepository.load(),
                        savedPrefRepository.getSvcidList()
                    )
                )
            } else {
                _synchronizationKey.postValue(
                    synchronizationRepository.upload(
                        idPrefRepository.load(),
                        namePrefRepository.load(),
                        savedPrefRepository.getSvcidList()
                    )
                )
            }
        }
    }

    fun getDataFromServer(key: String) {
        viewModelScope.launch(Dispatchers.IO) {
            storeKeyToPref(key)
            val data = synchronizationRepository.download(key)
            if (data.id.isNotEmpty() && data.name.isNotEmpty()) {
                idPrefRepository.save(data.id)
                namePrefRepository.save(data.name)
                savedPrefRepository.setSvcidList(data.savedServiceList)
            }

            _synchronizationData.postValue(data)
        }
    }

    fun storeKeyToPref(key: String) {
        synchronizationPrefRepository.save(key)
    }

    companion object {
        /** 뷰모델팩토리에서 의존성주입을 해준다 */
        val factory = viewModelFactory {
            initializer {
                val application =
                    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SeoulPublicServiceApplication)
                val container = application.container
                SettingViewModel(
                    idPrefRepository = container.idPrefRepository,
                    savedPrefRepository = container.savedPrefRepository,
                    synchronizationRepository = container.synchronizationRepository,
                    synchronizationPrefRepository = container.synchronizationPrefRepository,
                    namePrefRepository = container.namePrefRepository
                )
            }
        }
    }
}