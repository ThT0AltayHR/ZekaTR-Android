package com.muhammed.zekatr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.muhammed.zekatr.databinding.ActivityPermissionsBinding

/**
 * Kullanicinin istedigi "ilk girişte izin isteyen bildirim/ekran" burasi.
 * Hicbir izni sessizce/kullanici bilgisi disinda ACMIYORUZ - hepsi sistemin
 * kendi izin diyaloglari/ayar ekranlari uzerinden, kullanicinin onayiyla olur.
 */
class PermissionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionsBinding
    private lateinit var prefs: Prefs

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* sonuc ne olursa olsun devam ediyoruz, zorlamiyoruz */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        binding = ActivityPermissionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        NotificationHelper.createChannels(this)

        binding.buttonBattery.setOnClickListener {
            PermissionsHelper.requestIgnoreBatteryOptimizations(this)
        }
        binding.buttonAutoStart.setOnClickListener {
            PermissionsHelper.openAutoStartSettings(this)
        }
        binding.buttonNotif.setOnClickListener {
            requestNotificationPermission()
        }
        binding.buttonPermContinue.setOnClickListener { finishAndProceed(enableBackground = true) }
        binding.buttonPermSkip.setOnClickListener { finishAndProceed(enableBackground = false) }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            if (!granted) notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun finishAndProceed(enableBackground: Boolean) {
        prefs.backgroundRefreshEnabled = enableBackground
        if (enableBackground) {
            // Kullanicinin ACIKCA onayladigi durumda hem periyodik WorkManager
            // gorevini hem de (bagli kaldigi surece) foreground servisini baslatir.
            BackgroundRefreshWorker.schedule(this)
            SelfImprovementService.start(this)
            NotificationHelper.showSetupNotification(this)
        }
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
