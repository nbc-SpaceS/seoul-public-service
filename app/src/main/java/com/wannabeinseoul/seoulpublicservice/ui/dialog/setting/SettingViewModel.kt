package com.wannabeinseoul.seoulpublicservice.ui.dialog.setting

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wannabeinseoul.seoulpublicservice.SeoulPublicServiceApplication
import com.wannabeinseoul.seoulpublicservice.databases.firestore.SynchronizationRepository
import com.wannabeinseoul.seoulpublicservice.pref.IdPrefRepository
import com.wannabeinseoul.seoulpublicservice.pref.SavedPrefRepository
import com.wannabeinseoul.seoulpublicservice.pref.SynchronizationPrefRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingViewModel(
    private val idPrefRepository: IdPrefRepository,
    private val savedPrefRepository: SavedPrefRepository,
    private val synchronizationRepository: SynchronizationRepository,
    private val synchronizationPrefRepository: SynchronizationPrefRepository
): ViewModel() {

    private val _synchronizationKey: MutableLiveData<String> = MutableLiveData()
    val synchronizationKey: LiveData<String> get() = _synchronizationKey

    fun storeDataToServer(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val key = synchronizationPrefRepository.load()
            if (key.isNotEmpty()) {
                _synchronizationKey.postValue(synchronizationRepository.revise(key, idPrefRepository.load(), name, savedPrefRepository.getSvcidList()))
            } else {
                _synchronizationKey.postValue(synchronizationRepository.upload(idPrefRepository.load(), name, savedPrefRepository.getSvcidList()))
            }
        }
    }

    fun storeKeyToPref(key: String) {
        synchronizationPrefRepository.save(key)
    }

    companion object {
        /** 뷰모델팩토리에서 의존성주입을 해준다 */
        val factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SeoulPublicServiceApplication)
                val container = application.container
                SettingViewModel(
                    idPrefRepository = container.idPrefRepository,
                    savedPrefRepository = container.savedPrefRepository,
                    synchronizationRepository = container.synchronizationRepository,
                    synchronizationPrefRepository = container.synchronizationPrefRepository
                )
            }
        }
    }
}