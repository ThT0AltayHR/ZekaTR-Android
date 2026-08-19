package com.muhammed.zekatr

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {

    const val CHANNEL_ID_SETUP = "zekatr_setup"
    const val CHANNEL_ID_SERVICE = "zekatr_service"
    private const val NOTIF_ID_SETUP = 1001

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val setupChannel = NotificationChannel(
            CHANNEL_ID_SETUP, "Kurulum", NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "İzin kurulum hatırlatmaları" }

        val serviceChannel = NotificationChannel(
            CHANNEL_ID_SERVICE, "Arka Plan Öğrenme", NotificationManager.IMPORTANCE_LOW
        ).apply { description = "ZekaTR internet olduğunda periyodik güncelleme yaparken gösterilir" }

        manager.createNotificationChannel(setupChannel)
        manager.createNotificationChannel(serviceChannel)
    }

    /**
     * Uygulama ilk acildiginda gosterilen, Ayarlar'a yonlendiren bildirim.
     * Kullanicinin ACIKCA gordugu, tikladiginda SettingsActivity'yi acan
     * gercek (sahte olmayan) bir sistem bildirimidir.
     */
    fun showSetupNotification(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                context, PermissionsHelper.NOTIFICATION_PERMISSION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val intent = Intent(context, SettingsActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SETUP)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle("ZekaTR kendini geliştirmeye devam etsin")
            .setContentText("Arka planda periyodik öğrenmenin kesintisiz çalışması için pil/otomatik başlatma izinlerini aç.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Yapay zekanın internet olduğunda periyodik olarak kendini güncelleyebilmesi için " +
                            "cihazının Ayarlar bölümünden pil optimizasyonu kısıtlamasını kaldırman ve otomatik " +
                            "başlatmaya izin vermen gerekiyor. Ayarlara gitmek için dokun."
                )
            )
            .addAction(0, "Ayarlara Git", pendingIntent)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_ID_SETUP, notification)
    }
}
