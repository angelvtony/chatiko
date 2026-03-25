package com.example.chatiko.ui.chat

import android.content.Context
import android.util.Log
import org.webrtc.*

class WebRtcManager(private val context: Context, private val webRtcListener: WebRtcListener) {

    interface WebRtcListener {
        fun onIceCandidate(candidate: IceCandidate)
        fun onOfferReady(sessionDescription: SessionDescription)
        fun onAnswerReady(sessionDescription: SessionDescription)
        fun onRemoteTrackAdded(track: VideoTrack)
    }

    private var factory: PeerConnectionFactory? = null
    var peerConnection: PeerConnection? = null

    private var eglBase: EglBase = EglBase.create()

    private var localVideoSource: VideoSource? = null
    private var localAudioSource: AudioSource? = null
    var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoCapturer: VideoCapturer? = null

    // Audio routing
    private var audioManager: android.media.AudioManager? = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
    private var savedAudioMode: Int = android.media.AudioManager.MODE_NORMAL
    private var savedIsSpeakerPhoneOn: Boolean = false

    init {
        initializePeerConnectionFactory(context)
        factory = createPeerConnectionFactory()
    }

    private fun initializePeerConnectionFactory(context: Context) {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)
    }

    private fun createPeerConnectionFactory(): PeerConnectionFactory {
        val videoEncoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        val videoDecoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)
        return PeerConnectionFactory.builder()
            .setVideoEncoderFactory(videoEncoderFactory)
            .setVideoDecoderFactory(videoDecoderFactory)
            .setOptions(PeerConnectionFactory.Options().apply {
                disableEncryption = false
                disableNetworkMonitor = true
            })
            .createPeerConnectionFactory()
    }

    fun initLocalSurfaceView(view: SurfaceViewRenderer) {
        view.init(eglBase.eglBaseContext, null)
        view.setMirror(true)
        view.setEnableHardwareScaler(true)
        view.setZOrderMediaOverlay(true)
    }

    fun initRemoteSurfaceView(view: SurfaceViewRenderer) {
        view.init(eglBase.eglBaseContext, null)
        view.setMirror(false)
        view.setEnableHardwareScaler(true)
    }

    fun prepareMedia(isVideoCall: Boolean) {
        val audioConstraints = MediaConstraints()
        localAudioSource = factory?.createAudioSource(audioConstraints)
        localAudioTrack = factory?.createAudioTrack("local_audio", localAudioSource)
        localAudioTrack?.setEnabled(true)

        if (isVideoCall) {
            videoCapturer = createCameraCapturer()
            localVideoSource = factory?.createVideoSource(videoCapturer!!.isScreencast)
            
            val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
            videoCapturer?.initialize(surfaceTextureHelper, context, localVideoSource!!.capturerObserver)
            videoCapturer?.startCapture(640, 480, 30)

            localVideoTrack = factory?.createVideoTrack("local_video", localVideoSource)
        }
        
        // Setup initial audio routing
        savedAudioMode = audioManager?.mode ?: android.media.AudioManager.MODE_NORMAL
        savedIsSpeakerPhoneOn = audioManager?.isSpeakerphoneOn ?: false
        
        audioManager?.mode = android.media.AudioManager.MODE_IN_COMMUNICATION
        audioManager?.isSpeakerphoneOn = isVideoCall // Speaker for video, earpiece for audio
    }

    fun startLocalVideo(view: SurfaceViewRenderer) {
        localVideoTrack?.addSink(view)
    }

    fun toggleMute(isMuted: Boolean) {
        localAudioTrack?.setEnabled(!isMuted)
    }

    fun toggleSpeaker(isSpeakerOn: Boolean) {
        audioManager?.isSpeakerphoneOn = isSpeakerOn
    }
    
    fun switchCamera() {
        if (videoCapturer is CameraVideoCapturer) {
            (videoCapturer as CameraVideoCapturer).switchCamera(null)
        }
    }

    private fun createCameraCapturer(): VideoCapturer? {
        val enumerator = if (Camera2Enumerator.isSupported(context)) {
            Camera2Enumerator(context)
        } else {
            Camera1Enumerator(true)
        }
        val deviceNames = enumerator.deviceNames
        // Try front camera
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }
        // Fallback to back camera
        for (deviceName in deviceNames) {
            if (enumerator.isBackFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }
        return null
    }

    fun createPeerConnection(isVideoCall: Boolean) {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("turn:openrelay.metered.ca:80")
                .setUsername("openrelayproject")
                .setPassword("openrelayproject")
                .createIceServer(),
            PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443")
                .setUsername("openrelayproject")
                .setPassword("openrelayproject")
                .createIceServer()
        )

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }

        peerConnection = factory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                webRtcListener.onIceCandidate(candidate)
            }
            override fun onAddStream(stream: MediaStream) {}
            override fun onRemoveStream(stream: MediaStream) {}
            override fun onDataChannel(channel: DataChannel) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {}
            override fun onSignalingChange(state: PeerConnection.SignalingState) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver, mediaStreams: Array<out MediaStream>) {
                if (receiver.track() is VideoTrack) {
                    webRtcListener.onRemoteTrackAdded(receiver.track() as VideoTrack)
                }
            }
        })

        localAudioTrack?.let { peerConnection?.addTrack(it, listOf("mediaStream")) }
        if (isVideoCall) {
            localVideoTrack?.let { peerConnection?.addTrack(it, listOf("mediaStream")) }
        }
    }

    fun createOffer() {
        val mediaConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(this, sdp)
                webRtcListener.onOfferReady(sdp)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String) { Log.e("WebRTC", "Offer failed: $error") }
            override fun onSetFailure(error: String) { Log.e("WebRTC", "Set local desc failed: $error") }
        }, mediaConstraints)
    }

    fun createAnswer() {
        val mediaConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }

        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(this, sdp)
                webRtcListener.onAnswerReady(sdp)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String) {}
            override fun onSetFailure(error: String) {}
        }, mediaConstraints)
    }

    fun setRemoteDescription(sdp: SessionDescription, onSuccess: () -> Unit) {
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {}
            override fun onSetSuccess() { onSuccess() }
            override fun onCreateFailure(error: String?) { Log.e("WebRTC", "Set remote desc failed: $error") }
            override fun onSetFailure(error: String?) { Log.e("WebRTC", "Set remote desc failed: $error") }
        }, sdp)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    fun destroy() {
        videoCapturer?.stopCapture()
        videoCapturer?.dispose()
        localVideoSource?.dispose()
        localAudioSource?.dispose()
        peerConnection?.close()
        factory?.dispose()
        eglBase.release()
        
        // Restore audio state
        audioManager?.mode = savedAudioMode
        audioManager?.isSpeakerphoneOn = savedIsSpeakerPhoneOn
    }
}
