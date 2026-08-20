package com.muhammed.zekatr

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.muhammed.zekatr.databinding.ActivityTrainingBinding

class TrainingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTrainingBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrainingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }
        binding.btnStartTraining.setOnClickListener { startTraining() }
    }

    private fun startTraining() {
        binding.btnStartTraining.isEnabled = false
        binding.progressBar.progress = 0
        binding.textStatus.text = "Hazırlanıyor…"
        TrainingEngine(this).run(
            onProgress = { p -> runOnUiThread { binding.progressBar.progress = p.percent; binding.textStatus.text = p.stage } },
            onDone = { result -> runOnUiThread { binding.progressBar.progress = 100; binding.textStatus.text = result; binding.btnStartTraining.isEnabled = true } }
        )
    }
}
