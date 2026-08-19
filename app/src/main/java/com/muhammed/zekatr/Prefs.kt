package com.muhammed.zekatr

import android.content.Context

/**
 * Uygulama ayarlarini (kullanici adi, dusunme seviyesi, ozellik anahtarlari)
 * SharedPreferences uzerinde saklayan basit yardimci sinif.
 */
class Prefs(context: Context) {

    private val secure = SecurePrefs(context.applicationContext)

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


    var modelProvider: ModelRouter.Provider
        get() = runCatching { ModelRouter.Provider.valueOf(sp.getString(KEY_MODEL_PROVIDER, ModelRouter.Provider.LOCAL.name)!!) }.getOrDefault(ModelRouter.Provider.LOCAL)
        set(value) = sp.edit().putString(KEY_MODEL_PROVIDER, value.name).apply()

    var modelName: String
        get() = sp.getString(KEY_MODEL_NAME, defaultModel(modelProvider)) ?: defaultModel(modelProvider)
        set(value) = sp.edit().putString(KEY_MODEL_NAME, value).apply()

    var modelApiKey: String?
        get() = secure.get("model_api_key")
        set(value) = secure.put("model_api_key", value)

    var modelEnabled: Boolean
        get() = sp.getBoolean(KEY_MODEL_ENABLED, false)
        set(value) = sp.edit().putBoolean(KEY_MODEL_ENABLED, value).apply()

    var locationEnabled: Boolean
        get() = sp.getBoolean(KEY_LOCATION_ENABLED, false)
        set(value) = sp.edit().putBoolean(KEY_LOCATION_ENABLED, value).apply()

    var weatherLastNotificationAt: Long
        get() = sp.getLong(KEY_WEATHER_NOTIFY, 0L)
        set(value) = sp.edit().putLong(KEY_WEATHER_NOTIFY, value).apply()

    var weatherLastCode: Int
        get() = sp.getInt(KEY_WEATHER_CODE, -999)
        set(value) = sp.edit().putInt(KEY_WEATHER_CODE, value).apply()

    var weatherLat: Double
        get() = java.lang.Double.longBitsToDouble(sp.getLong(KEY_WEATHER_LAT, java.lang.Double.doubleToRawLongBits(Double.NaN)))
        set(value) = sp.edit().putLong(KEY_WEATHER_LAT, java.lang.Double.doubleToRawLongBits(value)).apply()

    var weatherLon: Double
        get() = java.lang.Double.longBitsToDouble(sp.getLong(KEY_WEATHER_LON, java.lang.Double.doubleToRawLongBits(Double.NaN)))
        set(value) = sp.edit().putLong(KEY_WEATHER_LON, java.lang.Double.doubleToRawLongBits(value)).apply()

    private fun defaultModel(provider: ModelRouter.Provider) = when (provider) {
        ModelRouter.Provider.GROQ -> "llama-3.3-70b-versatile"
        ModelRouter.Provider.OPENROUTER -> "openai/gpt-4o-mini"
        ModelRouter.Provider.OPENAI -> "gpt-4o-mini"
        ModelRouter.Provider.GEMINI -> "gemini-2.0-flash"
        ModelRouter.Provider.CLAUDE -> "claude-3-5-sonnet-latest"
        ModelRouter.Provider.LOCAL -> "local"
    }

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
        private const val KEY_MODEL_PROVIDER = "model_provider"
        private const val KEY_MODEL_NAME = "model_name"
        private const val KEY_MODEL_ENABLED = "model_enabled"
        private const val KEY_LOCATION_ENABLED = "location_enabled"
        private const val KEY_WEATHER_CODE = "weather_last_code"
        private const val KEY_WEATHER_NOTIFY = "weather_last_notify"
        private const val KEY_WEATHER_LAT = "weather_lat"
        private const val KEY_WEATHER_LON = "weather_lon"
    }
}
