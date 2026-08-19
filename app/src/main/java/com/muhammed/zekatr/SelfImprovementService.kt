package com.muhammed.zekatr

import android.app.Service
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

/**
 * "7/24 internet oldugu surece durmadan calissin, internet gidince otomatik
 * dursun, gelince otomatik devam etsin" istegine karsilik gelen GERCEK
 * (sahte olmayan) uygulama.
 *
 * Neyi gercekten yapiyor:
 *  - ConnectivityManager.NetworkCallback ile baglanti durumunu GERCEK ZAMANLI
 *    dinler. Internet giderse dongu otomatik durur (onLost), gelirse otomatik
 *    devam eder (onAvailable). Bu kisim tam istedigin gibi calisir.
 *  - Servis, kullanicinin ACIKCA gordugu bir bildirim (Android'in zorunlu
 *    tuttugu "foreground service" bildirimi) ile ayakta kalir; kullanici
 *    Ayarlar > Arka Plan Ogrenme'yi kapatirsa ya da bildirimi kapatirsa durur.
 *  - Pil optimizasyonundan muaf tutulmus ve otomatik baslatmaya izin verilmis
 *    bir cihazda, sistem bu servisi genelde kesmez; bu da pratikte
 *    "internet oldugu surece calisir" hedefine cok yaklasir.
 *
 * Neyi yapmiyor (ve neden):
 *  - Isleri "saniyede bir / anlik" tekrarlamiyor. Bunun yerine makul bir
 *    periyotla (varsayilan 10 dakika) calisiyor. Bunun nedeni durustluk:
 *    saniyede bir internet taramasi hem pili birkaç saatte tuketir, hem de
 *    hicbir gercek fayda saglamaz (o kadar sik yeni bilgi olusmaz), hem de
 *    Google Play politikalarina aykiridir. Bu sureyi SelfImprovementService
 *    icindeki INTERVAL_MS sabitinden degistirebilirsin.
 */
class SelfImprovementService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var connectivityManager: ConnectivityManager
    private var isOnline = false
    private var isRunningLoop = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            isOnline = true
            updateNotification("Bağlı — periyodik güncelleme aktif")
            startLoopIfNeeded()
        }

        override fun onLost(network: Network) {
            isOnline = false
            updateNotification("Bağlantı yok — internet gelince otomatik devam edecek")
            stopLoop()
        }
    }

    private val workRunnable = object : Runnable {
        override fun run() {
            if (!isOnline) return
            val prefs = Prefs(applicationContext)
            if (!prefs.backgroundRefreshEnabled) {
                stopSelfCompletely()
                return
            }
            doOneRealUpdate()
            handler.postDelayed(this, INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        connectivityManager = getSystemService(ConnectivityManager::class.java)
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)

        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        isOnline = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        startForeground(NOTIF_ID, buildNotification(if (isOnline) "Bağlı — periyodik güncelleme aktif" else "Bağlantı bekleniyor…"))
        if (isOnline) startLoopIfNeeded()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun startLoopIfNeeded() {
        if (isRunningLoop) return
        isRunningLoop = true
        handler.post(workRunnable)
    }

    private fun stopLoop() {
        isRunningLoop = false
        handler.removeCallbacks(workRunnable)
    }

    private fun doOneRealUpdate() {
        Thread {
            try {
                val prefs = Prefs(applicationContext)
                if (!prefs.webSearchEnabled) return@Thread
                val term = TERMS.random()
                val result = WebSearchHelper.search(term)
                if (result != null) {
                    val memory = LearnedData(applicationContext)
                    memory.teach(term.lowercase(), result.summary)
                }
            } catch (e: Exception) {
                // sessiz gec, bir sonraki periyotta tekrar denenir
            }
        }.start()
    }

    private fun updateNotification(status: String) {
        val manager = getSystemService(android.app.NotificationManager::class.java)
        manager.notify(NOTIF_ID, buildNotification(status))
    }

    private fun buildNotification(status: String): android.app.Notification {
        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID_SERVICE)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle("ZekaTR arka planda")
            .setContentText(status)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun stopSelfCompletely() {
        stopLoop()
        try { connectivityManager.unregisterNetworkCallback(networkCallback) } catch (e: Exception) { }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLoop()
        try { connectivityManager.unregisterNetworkCallback(networkCallback) } catch (e: Exception) { }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIF_ID = 2001
        private const val INTERVAL_MS = 10 * 60 * 1000L // 10 dakika - degistirilebilir, ama "aninda/surekli" degil

        private val TERMS = listOf(
            "güncel teknoloji haberleri", "bilim haberleri", "yapay zeka gelişmeleri",
            "Türkiye gündemi", "yeni kelime anlamı"
        )

        fun start(context: android.content.Context) {
            val intent = Intent(context, SelfImprovementService::class.java)
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: android.content.Context) {
            context.stopService(Intent(context, SelfImprovementService::class.java))
        }
    }
}
