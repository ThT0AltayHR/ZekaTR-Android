package com.muhammed.zekatr

import android.os.Handler
import android.os.Looper
import android.widget.TextView

/**
 * Metni harf harf "yaziliyormus" gibi gosteren basit bir animasyon.
 * Bu SADECE bir UI efektidir; arka planda gercekten adim adim "dusunen"
 * ayri bir model calistirmaz - bunu boyle sunmuyoruz, sadece okuma
 * deneyimini yumusatan kozmetik bir detay.
 */
object TypingAnimator {

    private val handler = Handler(Looper.getMainLooper())

    fun animate(view: TextView, fullText: String, charsPerTick: Int = 3, tickMs: Long = 12L, onDone: (() -> Unit)? = null) {
        handler.removeCallbacksAndMessages(view)
        var index = 0
        view.text = ""
        val runnable = object : Runnable {
            override fun run() {
                index = (index + charsPerTick).coerceAtMost(fullText.length)
                view.text = fullText.substring(0, index)
                if (index < fullText.length) {
                    handler.postAtTime(this, view, android.os.SystemClock.uptimeMillis() + tickMs)
                } else {
                    onDone?.invoke()
                }
            }
        }
        handler.postAtTime(runnable, view, android.os.SystemClock.uptimeMillis() + tickMs)
    }

    fun cancel(view: TextView) {
        handler.removeCallbacksAndMessages(view)
    }
}
