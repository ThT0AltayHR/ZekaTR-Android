package com.muhammed.zekatr

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.muhammed.zekatr.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: Prefs

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val account = GoogleAuthHelper.handleSignInResult(result.data)
        updateGoogleStatus()
        if (account == null) {
            android.widget.Toast.makeText(this, "Giriş başarısız oldu ya da iptal edildi.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.editSettingsName.setText(prefs.userName.orEmpty())
        binding.editSettingsName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) prefs.userName = binding.editSettingsName.text?.toString()?.trim()
        }

        when (prefs.thinkingLevel) {
            Prefs.ThinkingLevel.FAST -> binding.radioFast.isChecked = true
            Prefs.ThinkingLevel.NORMAL -> binding.radioNormal.isChecked = true
            Prefs.ThinkingLevel.DEEP -> binding.radioDeep.isChecked = true
        }
        binding.radioThinking.setOnCheckedChangeListener { _, checkedId ->
            prefs.thinkingLevel = when (checkedId) {
                R.id.radioFast -> Prefs.ThinkingLevel.FAST
                R.id.radioDeep -> Prefs.ThinkingLevel.DEEP
                else -> Prefs.ThinkingLevel.NORMAL
            }
        }

        binding.switchWebSearch.isChecked = prefs.webSearchEnabled
        binding.switchWebSearch.setOnCheckedChangeListener { _, checked -> prefs.webSearchEnabled = checked }

        binding.switchBgRefresh.isChecked = prefs.backgroundRefreshEnabled
        binding.switchBgRefresh.setOnCheckedChangeListener { _, checked ->
            prefs.backgroundRefreshEnabled = checked
            if (checked) {
                BackgroundRefreshWorker.schedule(this)
                SelfImprovementService.start(this)
            } else {
                BackgroundRefreshWorker.cancel(this)
                SelfImprovementService.stop(this)
            }
        }

        updateGoogleStatus()
        binding.buttonGoogle.setOnClickListener {
            val account = GoogleAuthHelper.currentAccount(this)
            if (account != null) {
                GoogleAuthHelper.signOut(this) { updateGoogleStatus() }
            } else {
                googleSignInLauncher.launch(GoogleAuthHelper.signInIntent(this))
            }
        }

        binding.buttonEmergency.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.settings_emergency_numbers))
                .setMessage(EmergencyContacts.listAllAsText())
                .setPositiveButton("Tamam", null)
                .show()
        }

        binding.buttonClearMemory.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.settings_clear_memory))
                .setMessage("Öğrenilen tüm bilgiler ve sohbet geçmişi silinecek. Emin misin?")
                .setPositiveButton("Sil") { _, _ ->
                    LearnedData(this).clearAll()
                    ChatHistoryStore(this).deleteAll()
                    android.widget.Toast.makeText(this, "Hafıza temizlendi.", android.widget.Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Vazgeç", null)
                .show()
        }
    }

    private fun updateGoogleStatus() {
        val account = GoogleAuthHelper.currentAccount(this)
        if (account != null) {
            binding.textGoogleStatus.text = account.email ?: "Giriş yapıldı"
            binding.buttonGoogle.text = getString(R.string.settings_sign_out)
        } else {
            binding.textGoogleStatus.text = "Giriş yapılmadı"
            binding.buttonGoogle.text = getString(R.string.settings_sign_in)
        }
    }
}
