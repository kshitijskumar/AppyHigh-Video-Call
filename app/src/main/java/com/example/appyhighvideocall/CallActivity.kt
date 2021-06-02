package com.example.appyhighvideocall

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.appyhighvideocall.databinding.ActivityCallBinding
import com.google.android.material.snackbar.Snackbar
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import java.lang.Exception

const val WAITING_TIME = 15000L
const val TICK_TIME = 1000L

class CallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCallBinding
    private val permissionsList = mutableListOf<String>()

    private val resultPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        val cameraPerm = it[Manifest.permission.CAMERA] ?: false
        val recordAudioPerm = it[Manifest.permission.RECORD_AUDIO] ?: false

        if(cameraPerm && recordAudioPerm) {
            initEngineAndJoinChannel()
        }
    }

    private var isCallInProgress: Boolean = false
    private var isMicEnabled: Boolean = true
    private var isVideoEnables: Boolean = true

    private val timer = object : CountDownTimer(WAITING_TIME, TICK_TIME) {
        override fun onTick(millisUntilFinished: Long) {
            Log.d("CallActivityTimer", "Time left: $millisUntilFinished")
        }

        override fun onFinish() {
            Toast.makeText(this@CallActivity, "Sorry, no one joined the random call.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private var rtcEngine: RtcEngine? = null

    private val rtcEventHandler = object : IRtcEngineEventHandler() {

        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            super.onJoinChannelSuccess(channel, uid, elapsed)

            runOnUiThread{
                Log.d("CallActivity", "Onjoinsucess: uuid: $uid")
                timerStart()
                isCallInProgress = true
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            super.onUserJoined(uid, elapsed)

            runOnUiThread {
                setupRemoteView(uid)
                timerStop()
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            super.onUserOffline(uid, reason)

            runOnUiThread {
                timerStart()
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestPermission()
        setupViews()

    }

    private fun setupViews() {
        binding.btnMute.setOnClickListener {
            toggleMic()
        }
        binding.btnEndCall.setOnClickListener {
            endCall()
        }
    }

    private fun endCall() {
        finish()
    }

    private fun initEngineAndJoinChannel() {
        initialiseEngine()
        setupLocalVideoView()
        joinChannel()
    }

    private fun initialiseEngine() {
        try {
            rtcEngine = RtcEngine.create(this, getString(R.string.agora_app_id), rtcEventHandler)
        }catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupLocalVideoView() {
        rtcEngine?.enableVideo()
        val surfaceView = RtcEngine.CreateRendererView(this)
        surfaceView.setZOrderMediaOverlay(true)
        binding.localView.addView(surfaceView)

        rtcEngine?.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, 0))
    }

    private fun joinChannel() {
        rtcEngine?.joinChannel(
            getString(R.string.agora_temp_token),
            "AppyHigh",
            "info",
            0
        )
    }

    private fun setupRemoteView(uid: Int) {
        if(binding.remoteView.childCount > 1) {
            return
        }
        val surfaceView = RtcEngine.CreateRendererView(this)
        binding.remoteView.addView(surfaceView)

        rtcEngine?.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, uid))
    }

    private fun toggleMic() {
        if(isMicEnabled) {
            binding.btnMute.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_mute_mic))

        }else {
            binding.btnMute.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_mic))
        }
        isMicEnabled = !isMicEnabled
        rtcEngine?.muteLocalAudioStream(isMicEnabled)
    }

    private fun timerStart() {
        timer.start()
        binding.progressbar.visibility = View.VISIBLE
    }

    private fun timerStop() {
        timer.cancel()
        binding.progressbar.visibility = View.GONE
    }

    private fun requestPermission() {
        val isCameraPermGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val isAudioPermGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

        if (!isCameraPermGranted) {
            permissionsList.add(Manifest.permission.CAMERA)
        }
        if(!isAudioPermGranted) {
            permissionsList.add(Manifest.permission.RECORD_AUDIO)
        }

        when {
            isCameraPermGranted && isAudioPermGranted -> {
                initEngineAndJoinChannel()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA
            ) -> {
                Snackbar.make(
                    binding.root,
                    "Camera permission is required to carry out the video call.",
                    Snackbar.LENGTH_INDEFINITE
                    ).setAction(
                        "Grant"
                    ) {
                        resultPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                }.show()
            }
            else -> {
                resultPermissionLauncher.launch(permissionsList.toTypedArray())
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        rtcEngine?.leaveChannel()
        RtcEngine.destroy()
    }

}