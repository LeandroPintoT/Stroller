package cl.panmu.stroller.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cl.panmu.stroller.util.ObsAudioItem
import cl.panmu.stroller.util.ObsSourceItem
import io.obswebsocket.community.client.OBSRemoteController
import java.time.LocalDateTime

class PageViewModel : ViewModel() {

    private val mutableObsController = MutableLiveData<OBSRemoteController>()
    private val mutableIsConnected = MutableLiveData(false)
    private val mutableIsStreaming = MutableLiveData(false)
    private val mutableIsRecording = MutableLiveData(false)
    private val mutableCurrScene = MutableLiveData("")
    private val mutableUsername = MutableLiveData("")
    private val mutableTimeStream = MutableLiveData<LocalDateTime>()
    private val mutableTimeRecord = MutableLiveData<LocalDateTime>()
    private val mutableAudioList = MutableLiveData<ArrayList<ObsAudioItem>>()
    private val mutableSourceList = MutableLiveData<ArrayList<ObsSourceItem>>()
    var obsController: LiveData<OBSRemoteController> = mutableObsController
    var isConnected: LiveData<Boolean> = mutableIsConnected
    var isStreaming: LiveData<Boolean> = mutableIsStreaming
    var isRecording: LiveData<Boolean> = mutableIsRecording
    var currScene: LiveData<String> = mutableCurrScene
    var username: LiveData<String> = mutableUsername
    var host: String = ""
    var timeStream: LiveData<LocalDateTime> = mutableTimeStream
    var timeRecord: LiveData<LocalDateTime> = mutableTimeRecord
    var audioList: LiveData<ArrayList<ObsAudioItem>> = mutableAudioList
    var sourceList: LiveData<ArrayList<ObsSourceItem>> = mutableSourceList

    fun obsController(item: OBSRemoteController) {
        mutableObsController.value = item
    }

    fun isConnected(b: Boolean) {
        mutableIsConnected.value = b
    }

    fun isStreaming(b: Boolean) {
        mutableIsStreaming.value = b
    }

    fun isRecording(b: Boolean) {
        mutableIsRecording.value = b
    }

    fun currScene(scene: String) {
        mutableCurrScene.value = scene
    }

    fun username(name: String) {
        mutableUsername.value = name
    }

    fun addToAudioList(scene: String, mul: Number) {
        mutableAudioList.value?.add(ObsAudioItem(scene, mul))
        mutableAudioList.value = mutableAudioList.value
    }

    fun clearAudioList() {
        mutableAudioList.value = ArrayList()
    }

    fun addToSourceList(nombreEscena: String, nombreFuente: String, idFuente: Number, parent: String) {
        mutableSourceList.value?.add(ObsSourceItem(nombreEscena, nombreFuente, idFuente, parent))
        mutableSourceList.value = mutableSourceList.value
    }

    fun clearSourceList() {
        mutableSourceList.value = ArrayList()
    }

    fun timeStream(time: LocalDateTime) {
        mutableTimeStream.value = time
    }

    fun timeRecord(time: LocalDateTime) {
        mutableTimeRecord.value = time
    }
}