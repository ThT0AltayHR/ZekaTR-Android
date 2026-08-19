package com.muhammed.zekatr

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * DURUST NOT: Bu fonksiyonlar kullanicinin ACIKCA izin verdigi, sistemin
 * KENDI ayar ekranlarini acar. Hicbirini biz "arkadan" veya kullanicinin
 * haberi olmadan aktif hale getiremeyiz - Android bunu izin vermez, ve
 * amacimiz da bu degil. Kullanici "Izin Ver" ekranlarinda "Izin Verme"
 * secebilir; bu durumda uygulama normal (kisitli) periyotta calismaya
 * devam eder, cokme veya hata vermez.
 */
object PermissionsHelper {

    /** Pil optimizasyonundan (Doze) muaf tutulma istegi - GERCEK sistem API'si. */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun requestIgnoreBatteryOptimizations(activity: Activity) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:${activity.packageName}")
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            openAppBatterySettingsFallback(activity)
        }
    }

    private fun openAppBatterySettingsFallback(activity: Activity) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:${activity.packageName}")
            activity.startActivity(intent)
        } catch (e: Exception) { /* sessizce yok say */
        }
    }

    /**
     * Bircok Android markasi (Xiaomi/MIUI, Huawei, Oppo/ColorOS, Vivo, Samsung vb.)
     * "Otomatik baslatma" icin kendi ozel ayar ekranlarini kullanir - bu ekranlar
     * Google'in standart API'sinde YOKTUR. Bilinen paket/aktivite adlariyla
     * en iyi cabayla acmayi deneriz; cihazda yoksa genel uygulama ayarlarina duseriz.
     */
    fun openAutoStartSettings(activity: Activity) {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val candidates = when {
            manufacturer.contains("xiaomi") -> listOf(
                "com.miui.securitycenter" to "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
            manufacturer.contains("huawei") -> listOf(
                "com.huawei.systemmanager" to "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            )
            manufacturer.contains("oppo") -> listOf(
                "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity"
            )
            manufacturer.contains("vivo") -> listOf(
                "com.vivo.permissionmanager" to "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
            )
            manufacturer.contains("samsung") -> listOf(
                "com.samsung.android.lool" to "com.samsung.android.sm.ui.battery.BatteryActivity"
            )
            else -> emptyList()
        }
        for ((pkg, cls) in candidates) {
            try {
                val intent = Intent()
                intent.component = android.content.ComponentName(pkg, cls)
                activity.startActivity(intent)
                return
            } catch (e: Exception) {
                // sirdaki adayi dene
            }
        }
        openAppBatterySettingsFallback(activity)
    }

    fun needsNotificationPermission(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    const val NOTIFICATION_PERMISSION = "android.permission.POST_NOTIFICATIONS"
}
