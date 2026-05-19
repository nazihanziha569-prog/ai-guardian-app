package com.example.ai_guardian.viewmodel

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.example.ai_guardian.data.model.Call
import com.example.ai_guardian.data.repository.CallRepository
import com.example.ai_guardian.data.repository.WebRTCRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import livekit.org.webrtc.EglBase
import livekit.org.webrtc.VideoTrack

class CallViewModel(
    private val repo: CallRepository = CallRepository()
) : ViewModel() {

    var calls     by mutableStateOf<List<Call>>(emptyList())
    var isLoading by mutableStateOf(false)

    private var listener  : ListenerRegistration? = null
    private var webRTCRepo: WebRTCRepository?      = null

    fun listenCallsByUid(uid: String) {
        isLoading = true
        listener = repo.listenCallsByUid(uid) { list ->
            calls     = list
            isLoading = false
        }
    }

    fun sendCall(from: String, to: String, callType: String = "video", onSuccess: (String) -> Unit = {}) {
        repo.createCall(from, to, callType) { onSuccess(it) }
    }

    // ✅ المتصل — يعمل offer ويكتبه في Firestore
    fun startCall(
        context      : Context,
        callId       : String,
        eglBase      : EglBase,
        onLocalVideo : (VideoTrack?) -> Unit,
        onRemoteVideo: (VideoTrack?) -> Unit,
        onOfferReady : () -> Unit = {}
    ) {
        webRTCRepo?.release()
        val r = WebRTCRepository(context).also { webRTCRepo = it }
        r.init(eglBase)
        r.onLocalStream  = onLocalVideo
        r.onRemoteStream = onRemoteVideo
        r.createLocalStream(eglBase)
        r.createPeerConnection(callId, isOffer = true)
        r.createOffer(callId) {
            onOfferReady()
            // ✅ اسمع الـ answer
            FirebaseFirestore.getInstance()
                .collection("calls").document(callId)
                .addSnapshotListener { snap, _ ->
                    val answer = snap?.getString("answer") ?: return@addSnapshotListener
                    if (answer.isNotEmpty()) r.setRemoteAnswer(answer)
                }
            // ✅ اسمع الـ ICE candidates تاع المستجيب
            FirebaseFirestore.getInstance()
                .collection("calls").document(callId)
                .collection("calleeCandidates")
                .addSnapshotListener { snap, _ ->
                    snap?.documentChanges?.forEach { ch ->
                        val c = ch.document
                        r.addIceCandidate(
                            c.getString("candidate") ?: return@forEach,
                            c.getString("sdpMid") ?: "",
                            (c.getLong("sdpMLineIndex") ?: 0).toInt()
                        )
                    }
                }
        }
    }

    // ✅ المستجيب — يقرأ الـ offer ويعمل answer
    fun answerCall(
        context      : Context,
        callId       : String,
        offerSdp     : String,
        eglBase      : EglBase,
        onLocalVideo : (VideoTrack?) -> Unit,
        onRemoteVideo: (VideoTrack?) -> Unit
    ) {
        if (offerSdp.isBlank()) return
        webRTCRepo?.release()
        val r = WebRTCRepository(context).also { webRTCRepo = it }
        r.init(eglBase)
        r.onLocalStream  = onLocalVideo
        r.onRemoteStream = onRemoteVideo
        r.createLocalStream(eglBase)
        r.createPeerConnection(callId, isOffer = false)
        r.createAnswer(callId, offerSdp) {
            // ✅ اسمع الـ ICE candidates تاع المتصل
            FirebaseFirestore.getInstance()
                .collection("calls").document(callId)
                .collection("callerCandidates")
                .addSnapshotListener { snap, _ ->
                    snap?.documentChanges?.forEach { ch ->
                        val c = ch.document
                        r.addIceCandidate(
                            c.getString("candidate") ?: return@forEach,
                            c.getString("sdpMid") ?: "",
                            (c.getLong("sdpMLineIndex") ?: 0).toInt()
                        )
                    }
                }
        }
    }

    // ✅ ربط الـ video callbacks بعد navigation
    fun attachVideoCallbacks(
        onLocalVideo : (VideoTrack?) -> Unit,
        onRemoteVideo: (VideoTrack?) -> Unit
    ) {
        webRTCRepo?.onLocalStream  = onLocalVideo
        webRTCRepo?.onRemoteStream = onRemoteVideo
        // أعد إطلاق الـ tracks إذا موجودة
        webRTCRepo?.reEmitTracks()
    }

    fun switchCamera()                 = webRTCRepo?.switchCamera()
    fun toggleMic(enabled: Boolean)    = webRTCRepo?.toggleMic(enabled)
    fun toggleCamera(enabled: Boolean) = webRTCRepo?.toggleCamera(enabled)

    fun endCall(callId: String) {
        webRTCRepo?.release()
        webRTCRepo = null
        FirebaseFirestore.getInstance()
            .collection("calls").document(callId)
            .update("status", "ended")
    }

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
        webRTCRepo?.release()
        webRTCRepo = null
    }

}
