package com.muhammed.zekatr

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.muhammed.zekatr.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)

        if (prefs.onboardingDone) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonContinue.setOnClickListener {
            val name = binding.editName.text?.toString()?.trim().orEmpty()
            if (name.isNotEmpty()) prefs.userName = name
            prefs.onboardingDone = true
            goToMain()
        }

        binding.buttonSkip.setOnClickListener {
            prefs.onboardingDone = true
            goToMain()
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, PermissionActivity::class.java))
        finish()
    }
}
