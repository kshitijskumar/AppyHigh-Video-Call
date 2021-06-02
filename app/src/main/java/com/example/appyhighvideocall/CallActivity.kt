package com.example.appyhighvideocall

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.appyhighvideocall.databinding.ActivityCallBinding
import com.google.android.material.snackbar.Snackbar

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestPermission()

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

    private fun initEngineAndJoinChannel() {

    }
}