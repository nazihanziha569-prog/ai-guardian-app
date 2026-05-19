package com.example.ai_guardian.data.repository


import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import livekit.org.webrtc.AudioTrack
import livekit.org.webrtc.Camera2Enumerator
import livekit.org.webrtc.CameraVideoCapturer
import livekit.org.webrtc.DataChannel
import livekit.org.webrtc.DefaultVideoDecoderFactory
import livekit.org.webrtc.DefaultVideoEncoderFactory
import livekit.org.webrtc.EglBase
import livekit.org.webrtc.IceCandidate
import livekit.org.webrtc.MediaConstraints
import livekit.org.webrtc.MediaStream
import livekit.org.webrtc.PeerConnection
import livekit.org.webrtc.PeerConnectionFactory
import livekit.org.webrtc.RtpReceiver
import livekit.org.webrtc.RtpTransceiver
import livekit.org.webrtc.SdpObserver
import livekit.org.webrtc.SessionDescription
import livekit.org.webrtc.SurfaceTextureHelper
import livekit.org.webrtc.VideoTrack
import kotlin.invoke

class WebRTCRepository(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()

    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null

    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null
    private var videoCapturer: CameraVideoCapturer? = null

    var onRemoteStream: ((VideoTrack?) -> Unit)? = null
    var onLocalStream: ((VideoTrack?) -> Unit)? = null

    companion object {
        private var isInitialized = false
    }

    // ───────────────────────── INIT ─────────────────────────
    fun init(eglBase: EglBase) {
        if (!isInitialized) {
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context)
                    .setEnableInternalTracer(true)
                    .createInitializationOptions()
            )
            isInitialized = true
        }

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        audioManager.mode = android.media.AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = false

        val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
    }

    // ───────────────────────── LOCAL STREAM ─────────────────────────
    fun createLocalStream(eglBase: EglBase): VideoTrack? {

        val audioConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
        }

        val audioSource = peerConnectionFactory.createAudioSource(audioConstraints)
        localAudioTrack = peerConnectionFactory.createAudioTrack("audio0", audioSource)
        localAudioTrack?.setEnabled(true) // ✅ تأكد يكون enabled


        videoCapturer = createCameraCapturer()
        if (videoCapturer == null) return null

        val surfaceHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
        val videoSource = peerConnectionFactory.createVideoSource(false)

        videoCapturer?.initialize(surfaceHelper, context, videoSource.capturerObserver)
        videoCapturer?.startCapture(1280, 720, 30)

        localVideoTrack = peerConnectionFactory.createVideoTrack("video0", videoSource)
        localVideoTrack?.setEnabled(true)

        onLocalStream?.invoke(localVideoTrack)

        return localVideoTrack
    }

    // ───────────────────────── PEER CONNECTION ─────────────────────────
    fun createPeerConnection(callId: String, isOffer: Boolean) {

        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun.relay.metered.ca:80").createIceServer(),
            PeerConnection.IceServer.builder("turn:global.relay.metered.ca:80")
                .setUsername("83eebabf8b4cce9d5dbcb649")
                .setPassword("2D7JvfkOQtBdYW3R")
                .createIceServer(),
            PeerConnection.IceServer.builder("turn:global.relay.metered.ca:80?transport=tcp")
                .setUsername("83eebabf8b4cce9d5dbcb649")
                .setPassword("2D7JvfkOQtBdYW3R")
                .createIceServer(),
            PeerConnection.IceServer.builder("turn:global.relay.metered.ca:443")
                .setUsername("83eebabf8b4cce9d5dbcb649")
                .setPassword("2D7JvfkOQtBdYW3R")
                .createIceServer(),
            PeerConnection.IceServer.builder("turns:global.relay.metered.ca:443?transport=tcp")
                .setUsername("83eebabf8b4cce9d5dbcb649")
                .setPassword("2D7JvfkOQtBdYW3R")
                .createIceServer()
        )

        val config = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics      = PeerConnection.SdpSemantics.UNIFIED_PLAN
            iceTransportsType = PeerConnection.IceTransportsType.ALL
            bundlePolicy      = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy     = PeerConnection.RtcpMuxPolicy.REQUIRE
        }

        peerConnection = peerConnectionFactory.createPeerConnection(
            config,
            object : PeerConnection.Observer {

                override fun onIceCandidate(candidate: IceCandidate) {
                    val sub = if (isOffer) "callerCandidates" else "calleeCandidates"
                    db.collection("calls").document(callId).collection(sub)
                        .add(mapOf(
                            "candidate"     to candidate.sdp,
                            "sdpMid"        to candidate.sdpMid,
                            "sdpMLineIndex" to candidate.sdpMLineIndex
                        ))
                }

                override fun onTrack(transceiver: RtpTransceiver?) {
                    val track = transceiver?.receiver?.track()
                    when (track) {
                        is VideoTrack -> { track.setEnabled(true); onRemoteStream?.invoke(track) }
                        is AudioTrack -> { track.setEnabled(true) }
                    }
                }

                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                    android.util.Log.d("ICE", "ICE: $state")
                }

                override fun onConnectionChange(state: PeerConnection.PeerConnectionState?) {
                    android.util.Log.d("ICE", "PC: $state")
                }

                override fun onSignalingChange(p0: PeerConnection.SignalingState?)       {}
                override fun onIceConnectionReceivingChange(p0: Boolean)                 {}
                override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
                override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?)        {}
                override fun onAddStream(p0: MediaStream?)                               {}
                override fun onRemoveStream(p0: MediaStream?)                            {}
                override fun onDataChannel(p0: DataChannel?)                             {}
                override fun onRenegotiationNeeded()                                     {}
                override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?)   {}
            }
        )

        localAudioTrack?.let { peerConnection?.addTrack(it) }
        localVideoTrack?.let { peerConnection?.addTrack(it) }
    }

    // ───────────────────────── OFFER ─────────────────────────
    fun createOffer(callId: String, onDone: () -> Unit) {
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {

                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        db.collection("calls")
                            .document(callId)
                            .set(mapOf("offer" to sdp.description), com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener { onDone() }
                    }

                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {}
                }, sdp)
            }

            override fun onCreateFailure(p0: String?) {}
            override fun onSetSuccess() {}
            override fun onSetFailure(p0: String?) {}

        }, MediaConstraints())
    }

    // ───────────────────────── CLEAN ─────────────────────────
    fun release() {
        videoCapturer?.stopCapture()
        videoCapturer?.dispose()

        localVideoTrack?.dispose()
        localAudioTrack?.dispose()

        peerConnection?.close()
        peerConnection = null


       val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        audioManager.mode = android.media.AudioManager.MODE_NORMAL
        audioManager.isSpeakerphoneOn = false
    }

    private fun createCameraCapturer(): CameraVideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        val device = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
        return device?.let { enumerator.createCapturer(it, null) }
    }
    // ===================== ANSWER =====================
    fun createAnswer(callId: String, offerSdp: String, onDone: () -> Unit) {
        val constraints = MediaConstraints()

        val session = SessionDescription(
            SessionDescription.Type.OFFER,
            offerSdp
        )

        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {

                peerConnection?.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(sdp: SessionDescription) {

                        peerConnection?.setLocalDescription(object : SdpObserver {
                            override fun onSetSuccess() {

                                db.collection("calls")
                                    .document(callId)
                                    .set(mapOf("answer" to sdp.description), com.google.firebase.firestore.SetOptions.merge())
                                    .addOnSuccessListener { onDone() }
                            }

                            override fun onCreateSuccess(p0: SessionDescription?) {}
                            override fun onCreateFailure(p0: String?) {}
                            override fun onSetFailure(p0: String?) {}
                        }, sdp)
                    }

                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetSuccess() {}
                    override fun onSetFailure(p0: String?) {}

                }, constraints)
            }

            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}

        }, session)
    }
    fun addIceCandidate(candidate: String, sdpMid: String, sdpMLineIndex: Int) {
        val ice = IceCandidate(sdpMid, sdpMLineIndex, candidate)
        peerConnection?.addIceCandidate(ice)
    }
    fun setRemoteAnswer(answer: String) {
        val session = SessionDescription(
            SessionDescription.Type.ANSWER,
            answer
        )
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {}
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        }, session)
    }
    fun switchCamera() {
        (videoCapturer as? CameraVideoCapturer)?.switchCamera(null)
    }

    fun toggleMic(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
    }

    fun toggleCamera(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
    }
    fun reEmitTracks() {
        localVideoTrack?.let  { onLocalStream?.invoke(it)  }
    }

}
