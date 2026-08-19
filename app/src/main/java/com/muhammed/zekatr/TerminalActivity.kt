package com.muhammed.zekatr

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.muhammed.zekatr.databinding.ActivityTerminalBinding

class TerminalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTerminalBinding
    private lateinit var terminal: TerminalService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTerminalBinding.inflate(layoutInflater)
        setContentView(binding.root)
        terminal = TerminalService(this)

        binding.btnBackTerminal.setOnClickListener { finish() }
        appendLine("ZekaTR Terminal — gerçek bir bash ortamı (Termux bootstrap).")

        setupTerminal()

        binding.btnRunCommand.setOnClickListener {
            val cmd = binding.editTerminalCommand.text.toString().trim()
            if (cmd.isEmpty()) return@setOnClickListener
            binding.editTerminalCommand.text.clear()
            runCommand(cmd)
        }
    }

    private fun setupTerminal() {
        lifecycleScope.launch {
            binding.textTerminalStatus.text = "kuruluyor..."
            val result = withContext(Dispatchers.IO) {
                terminal.install { progress -> runOnUiThread { binding.textTerminalStatus.text = progress } }
            }
            result.onSuccess {
                binding.textTerminalStatus.text = "hazır ✓"
                appendLine("✓ Bootstrap kuruldu. python/git gerekirse otomatik kurulacak (apt).")
            }.onFailure { e ->
                binding.textTerminalStatus.text = "hata"
                appendLine("✗ Kurulum hatası: ${e.message}")
            }
        }
    }

    private fun runCommand(cmd: String) {
        appendLine("\n$ $cmd")
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { terminal.exec(cmd) }
            if (result.stdout.isNotBlank()) appendLine(result.stdout.trimEnd())
            if (result.stderr.isNotBlank()) appendLine(result.stderr.trimEnd())
            appendLine("[exit ${result.exitCode}]")
        }
    }

    private fun appendLine(text: String) {
        binding.textTerminalOutput.append("$text\n")
    }
}
