package com.example.appyhighvideocall

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.appyhighvideocall.databinding.ActivityMainBinding
import com.google.android.gms.ads.MobileAds

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MobileAds.initialize(this)
        setupViews()
    }

    private fun setupViews() {
        binding.btnGetStarted.setOnClickListener {
            val intent = Intent(this, CallActivity::class.java)
            startActivity(intent)
        }
    }
}