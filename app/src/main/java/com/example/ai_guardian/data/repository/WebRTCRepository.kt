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

        val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
    }

    // ───────────────────────── LOCAL STREAM ─────────────────────────
    fun createLocalStream(eglBase: EglBase): VideoTrack? {

        val audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory.createAudioTrack("audio0", audioSource)

        videoCapturer = createCameraCapturer()
        if (videoCapturer == null) return null

        val surfaceHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
        val videoSource = peerConnectionFactory.createVideoSource(false)

        videoCapturer?.initialize(surfaceHelper, context, videoSource.capturerObserver)
        videoCapturer?.startCapture(1280, 720, 30)

        localVideoTrack = peerConnectionFactory.createVideoTrack("video0", videoSource)

        onLocalStream?.invoke(localVideoTrack)

        return localVideoTrack
    }

    // ───────────────────────── PEER CONNECTION ─────────────────────────
    fun createPeerConnection(callId: String, isOffer: Boolean) {

        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )

        val config = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }

        peerConnection = peerConnectionFactory.createPeerConnection(
            config,
            object : PeerConnection.Observer {

                override fun onIceCandidate(candidate: IceCandidate) {

                    val sub = if (isOffer) "callerCandidates" else "calleeCandidates"

                    db.collection("calls")
                        .document(callId)
                        .collection(sub)
                        .add(
                            mapOf(
                                "candidate" to candidate.sdp,
                                "sdpMid" to candidate.sdpMid,
                                "sdpMLineIndex" to candidate.sdpMLineIndex
                            )
                        )
                }

                override fun onTrack(transceiver: RtpTransceiver?) {
                    val track = transceiver?.receiver?.track()
                    if (track is VideoTrack) {
                        onRemoteStream?.invoke(track)
                    }
                }

                override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
                override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {}
                override fun onIceConnectionReceivingChange(p0: Boolean) {}
                override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
                override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
                override fun onAddStream(p0: MediaStream?) {}
                override fun onRemoveStream(p0: MediaStream?) {}
                override fun onDataChannel(p0: DataChannel?) {}
                override fun onRenegotiationNeeded() {}
                override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {}
                override fun onConnectionChange(p0: PeerConnection.PeerConnectionState?) {}
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
                            .update("offer", sdp.description)
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
                                    .update("answer", sdp.description)
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
}