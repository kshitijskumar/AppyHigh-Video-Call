package com.example.appyhighvideocall

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.appyhighvideocall.data.Repository
import com.example.appyhighvideocall.data.TokenInfo
import com.example.appyhighvideocall.databinding.ActivityCallBinding
import com.google.android.material.snackbar.Snackbar
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import kotlinx.coroutines.launch
import java.lang.Exception

const val WAITING_TIME = 15000L
const val TICK_TIME = 1000L

class CallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCallBinding
    private val permissionsList = mutableListOf<String>()

    private val repo by lazy {
        Repository()
    }

    private val resultPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        val cameraPerm = it[Manifest.permission.CAMERA] ?: false
        val recordAudioPerm = it[Manifest.permission.RECORD_AUDIO] ?: false

        if(cameraPerm && recordAudioPerm) {
            initEngineAndJoinChannel()
        }
    }

    private var isTimerOn: Boolean = false
    private var isMicEnabled: Boolean = true
    private var isVideoEnabled: Boolean = true

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


    //callback to handle agora events for videocall
    private val rtcEventHandler = object : IRtcEngineEventHandler() {

        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            super.onJoinChannelSuccess(channel, uid, elapsed)

            runOnUiThread{
                Log.d("CallActivity", "Onjoinsucess: uuid: $uid")
                timerStart()
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
                Toast.makeText(this@CallActivity, "The user left the call.", Toast.LENGTH_LONG).show()
                timerStart()
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        requestPermission()
        setupViews()
    }

    //setup views and click listeners
    private fun setupViews() {
        binding.btnMute.setOnClickListener {
            toggleMic()
        }

        binding.btnVideo.setOnClickListener {
            toggleVid()
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
        getCurrentActiveTokenAndJoin()
    }

    //initialises the rtc engine
    private fun initialiseEngine() {
        try {
            rtcEngine = RtcEngine.create(this, getString(R.string.agora_app_id), rtcEventHandler)
        }catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //setup local view to show user video
    private fun setupLocalVideoView() {
        rtcEngine?.enableVideo()
        val surfaceView = RtcEngine.CreateRendererView(this)
        surfaceView.setZOrderMediaOverlay(true)
        binding.localView.addView(surfaceView)

        rtcEngine?.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, 0))
    }


    //retrieves active agora token from firebase firestore, and if it is not null, then joins the corresponding channel
    private fun getCurrentActiveTokenAndJoin() = lifecycleScope.launch {
        val tokenInfo = repo.getCurrentActiveToken()
        if(tokenInfo == null) {
            Toast.makeText(this@CallActivity, "Token not found", Toast.LENGTH_LONG).show()
            finish()
        }else {
            Log.d("CallActivity", "Active token is $tokenInfo")
            Log.d("CallActivity", "Thread is: ${Thread.currentThread().name}")
            joinChannel(tokenInfo)
        }
    }

    //joins the channel with given token and name of the channel
    private fun joinChannel(tokenInfo: TokenInfo) {
        rtcEngine?.joinChannel(
            tokenInfo.token,
            tokenInfo.name,
            "info",
            0
        )
    }

    //set up remote view
    private fun setupRemoteView(uid: Int) {
        if(binding.remoteView.childCount > 1) {
            return
        }
        val surfaceView = RtcEngine.CreateRendererView(this)
        binding.remoteView.addView(surfaceView)

        rtcEngine?.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, uid))
    }

    //toggles the mic for mute and unmute
    private fun toggleMic() {
        if(isMicEnabled) {
            binding.btnMute.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_mute_mic))

        }else {
            binding.btnMute.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_mic))
        }
        rtcEngine?.muteLocalAudioStream(isMicEnabled)
        isMicEnabled = !isMicEnabled
    }

    //toggles the camera for video on and video off
    private fun toggleVid() {
        if(isVideoEnabled) {
            binding.btnVideo.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_video_off))

        }else {
            binding.btnVideo.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_vid))
        }
        rtcEngine?.muteLocalVideoStream(isVideoEnabled)
        isVideoEnabled = !isVideoEnabled
    }


    //starts the timer of 15sec to hold the user till someone joins.
    private fun timerStart() {
        timer.start()
        isTimerOn = true
        binding.progressbar.visibility = View.VISIBLE
    }

    //stops the timer when someone on the remote side joins the same channel
    private fun timerStop() {
        timer.cancel()
        isTimerOn = false
        binding.progressbar.visibility = View.GONE
    }

    //checks whether the permissions are grantd or not, if not then asks for those permissions
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

    //when this activity destroys, leave the channel, destroy Rtc and checks if timer is running then stops that timer
    override fun onDestroy() {
        super.onDestroy()
        rtcEngine?.leaveChannel()
        RtcEngine.destroy()
        if (isTimerOn) {
            timerStop()
        }
    }

}