package com.muhammed.zekatr

import android.content.Context

/**
 * Uygulama ayarlarini (kullanici adi, dusunme seviyesi, ozellik anahtarlari)
 * SharedPreferences uzerinde saklayan basit yardimci sinif.
 */
class Prefs(context: Context) {

    private val sp = context.applicationContext.getSharedPreferences("zekatr_prefs", Context.MODE_PRIVATE)

    var userName: String?
        get() = sp.getString(KEY_NAME, null)
        set(value) = sp.edit().putString(KEY_NAME, value).apply()

    var onboardingDone: Boolean
        get() = sp.getBoolean(KEY_ONBOARDING, false)
        set(value) = sp.edit().putBoolean(KEY_ONBOARDING, value).apply()

    /** THINKING_FAST: dogrudan cevap. THINKING_NORMAL: kisa "dusunuyor" adimi.
     *  THINKING_DEEP: analiz + (izinliyse) web aramasi adimlarini gosterir. */
    var thinkingLevel: ThinkingLevel
        get() = ThinkingLevel.values().getOrElse(sp.getInt(KEY_THINKING, 1)) { ThinkingLevel.NORMAL }
        set(value) = sp.edit().putInt(KEY_THINKING, value.ordinal).apply()

    /** Kullanici web aramasini acikca ACTIVATE ettiyse true olur.
     *  Varsayilan kapalidir; internet erisimi gerektiren her ozellik opt-in'dir. */
    var webSearchEnabled: Boolean
        get() = sp.getBoolean(KEY_WEB_SEARCH, false)
        set(value) = sp.edit().putBoolean(KEY_WEB_SEARCH, value).apply()

    var backgroundRefreshEnabled: Boolean
        get() = sp.getBoolean(KEY_BG_REFRESH, false)
        set(value) = sp.edit().putBoolean(KEY_BG_REFRESH, value).apply()

    var activeSessionId: String
        get() = sp.getString(KEY_ACTIVE_SESSION, null) ?: run {
            val id = java.util.UUID.randomUUID().toString()
            activeSessionId = id
            id
        }
        set(value) = sp.edit().putString(KEY_ACTIVE_SESSION, value).apply()

    enum class ThinkingLevel { FAST, NORMAL, DEEP }

    companion object {
        private const val KEY_NAME = "user_name"
        private const val KEY_ONBOARDING = "onboarding_done"
        private const val KEY_THINKING = "thinking_level"
        private const val KEY_WEB_SEARCH = "web_search_enabled"
        private const val KEY_BG_REFRESH = "bg_refresh_enabled"
        private const val KEY_ACTIVE_SESSION = "active_session_id"
    }
}
