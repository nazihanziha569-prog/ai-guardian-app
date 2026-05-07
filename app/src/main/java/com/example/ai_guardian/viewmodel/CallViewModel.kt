package com.example.ai_guardian.viewmodel

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai_guardian.data.model.Call
import com.example.ai_guardian.data.repository.CallRepository
import com.example.ai_guardian.data.repository.WebRTCRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import livekit.org.webrtc.EglBase
import livekit.org.webrtc.VideoTrack


class CallViewModel(
    private val repo: CallRepository = CallRepository()
) : ViewModel() {

    var calls     by mutableStateOf<List<Call>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set


    private var listener  : ListenerRegistration? = null
    private var webRTCRepo: WebRTCRepository?      = null


    // ── Écouter les appels d'un uid ───────────────────────────────────────
    fun listenCallsByUid(uid: String) {
        isLoading = true
        listener = repo.listenCallsByUid(uid) { rawCalls ->

            viewModelScope.launch(Dispatchers.IO) {

                val resolved = rawCalls.map { call ->
                    call.copy(
                        from = call.from, // ❌ pas resolve ici
                        to   = call.to
                    )
                }

                launch(Dispatchers.Main) {
                    calls     = resolved
                    isLoading = false
                }
            }
        }
    }

    // ── Écouter les appels entre deux personnes ───────────────────────────
    fun listenCallsBetween(uid1: String, uid2: String) {
        isLoading = true
        listener = repo.listenCallsBetween(uid1, uid2) { result ->
            calls     = result
            isLoading = false
        }
    }

    // ── Créer un appel (sans WebRTC — juste le doc Firestore) ─────────────
    fun sendCall(
        from     : String,
        to       : String,
        callType : String = "video",
        onSuccess: (String) -> Unit = {}
    ) {
        repo.createCall(from, to, callType) { callId -> onSuccess(callId) }
    }

    // ─────────────────────────────────────────────────────────────────────
    // ✅ FIX: startCall — crée l'offer ET l'écrit dans Firestore
    //         onOfferReady est appelé SEULEMENT quand offer est dans Firestore
    //         → c'est là que l'appelant navigue vers VideoCallScreen
    // ─────────────────────────────────────────────────────────────────────
    fun startCall(
        context      : Context,
        callId       : String,
        eglBase      : EglBase,
        onLocalVideo : (VideoTrack?) -> Unit,
        onRemoteVideo: (VideoTrack?) -> Unit,
        onOfferReady : () -> Unit = {}          // ✅ nouveau callback
    ) {
        val r = WebRTCRepository(context).also { webRTCRepo = it }
        r.init(eglBase)                          // ✅ un seul EglBase
        r.onLocalStream  = onLocalVideo
        r.onRemoteStream = onRemoteVideo
        r.createLocalStream(eglBase)
        r.createPeerConnection(callId, isOffer = true)

        r.createOffer(callId) {
            // ✅ Offer écrit dans Firestore → on peut naviguer maintenant
            onOfferReady()

            // Écouter answer de l'appelé
            FirebaseFirestore.getInstance()
                .collection("calls").document(callId)
                .addSnapshotListener { snap, _ ->
                    val answer = snap?.getString("answer") ?: return@addSnapshotListener
                    if (answer.isNotEmpty()) r.setRemoteAnswer(answer)
                }

            // Écouter ICE candidates de l'appelé
            FirebaseFirestore.getInstance()
                .collection("calls").document(callId)
                .collection("calleeCandidates")
                .addSnapshotListener { snap, _ ->
                    snap?.documentChanges?.forEach { change ->
                        val c = change.document
                        r.addIceCandidate(
                            c.getString("candidate") ?: return@forEach,
                            c.getString("sdpMid")    ?: "",
                            (c.getLong("sdpMLineIndex") ?: 0).toInt()
                        )
                    }
                }
        }
    }

    private var pendingLocalVideo  : ((VideoTrack?) -> Unit)? = null
    private var pendingRemoteVideo : ((VideoTrack?) -> Unit)? = null

    fun attachVideoCallbacks(
        onLocalVideo : (VideoTrack?) -> Unit,
        onRemoteVideo: (VideoTrack?) -> Unit
    ) {
        pendingLocalVideo  = onLocalVideo
        pendingRemoteVideo = onRemoteVideo
        webRTCRepo?.onLocalStream  = onLocalVideo
        webRTCRepo?.onRemoteStream = onRemoteVideo
    }

    // ─────────────────────────────────────────────────────────────────────
    // ✅ FIX: answerCall — offerSdp doit être non-vide
    // ─────────────────────────────────────────────────────────────────────
    fun answerCall(
        context      : Context,
        callId       : String,
        offerSdp     : String,
        eglBase      : EglBase,
        onLocalVideo : (VideoTrack?) -> Unit,
        onRemoteVideo: (VideoTrack?) -> Unit
    ) {
        if (offerSdp.isBlank()) {
            android.util.Log.e("CallVM", "❌ offerSdp est vide — answerCall annulé")
            return
        }

        val r = WebRTCRepository(context).also { webRTCRepo = it }
        r.init(eglBase)                          // ✅ un seul EglBase
        r.onLocalStream  = onLocalVideo
        r.onRemoteStream = onRemoteVideo
        r.createLocalStream(eglBase)
        r.createPeerConnection(callId, isOffer = false)

        r.createAnswer(callId, offerSdp) {
            // Écouter ICE candidates de l'appelant
            FirebaseFirestore.getInstance()
                .collection("calls").document(callId)
                .collection("callerCandidates")
                .addSnapshotListener { snap, _ ->
                    snap?.documentChanges?.forEach { change ->
                        val c = change.document
                        r.addIceCandidate(
                            c.getString("candidate") ?: return@forEach,
                            c.getString("sdpMid")    ?: "",
                            (c.getLong("sdpMLineIndex") ?: 0).toInt()
                        )
                    }
                }
        }
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
        listener = null

        webRTCRepo?.release()
        webRTCRepo = null
    }


}