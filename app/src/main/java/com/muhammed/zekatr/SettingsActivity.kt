package com.muhammed.zekatr

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.muhammed.zekatr.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: Prefs

    private val locationLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

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

        val providers = ModelRouter.Provider.values().map { it.label }
        binding.spinnerProvider.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, providers)
        binding.spinnerProvider.setSelection(ModelRouter.Provider.values().indexOf(prefs.modelProvider).coerceAtLeast(0))
        binding.editModelName.setText(prefs.modelName)
        binding.editModelKey.setText(prefs.modelApiKey.orEmpty())
        binding.switchModel.isChecked = prefs.modelEnabled
        binding.spinnerProvider.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val p = ModelRouter.Provider.values()[position]
                prefs.modelProvider = p
                if (binding.editModelName.text.isNullOrBlank() || binding.editModelName.text.toString() == "local") binding.editModelName.setText(prefs.modelName)
            }
        })
        binding.switchModel.setOnCheckedChangeListener { _, checked -> prefs.modelEnabled = checked }
        binding.editModelName.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) prefs.modelName = binding.editModelName.text?.toString()?.trim().orEmpty() }
        binding.editModelKey.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) prefs.modelApiKey = binding.editModelKey.text?.toString()?.trim() }
        binding.switchLocation.isChecked = prefs.locationEnabled
        binding.switchLocation.setOnCheckedChangeListener { _, checked ->
            prefs.locationEnabled = checked
            if (checked) { WeatherNewsWorker.schedule(this); requestLocation() }
        }
        binding.buttonTraining.setOnClickListener { startActivity(Intent(this, TrainingActivity::class.java)) }

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

    private fun requestLocation() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            locationLauncher.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    override fun onPause() {
        super.onPause()
        prefs.modelName = binding.editModelName.text?.toString()?.trim().orEmpty()
        prefs.modelApiKey = binding.editModelKey.text?.toString()?.trim()
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
