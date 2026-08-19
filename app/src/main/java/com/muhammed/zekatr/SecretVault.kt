package com.muhammed.zekatr

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * SecretVault
 * -----------
 * Replit "Secrets" tarzı, çoklu adlandırılmış token/kasa yönetimi.
 * - Her değer AndroidKeyStore'daki donanım destekli AES/GCM anahtarıyla şifrelenir.
 * - Ham (çözülmüş) değer sadece bu sınıf içinde, kullanıldığı anda belleğe alınır;
 *   arayüz katmanına asla düz metin olarak geri döndürülmez (sadece maskelenmiş önizleme).
 * - Terminal / Git entegrasyonu gibi işlemler `useSecret(name) { plain -> ... }`
 *   ile çağrılır: plain değer sadece o lambda kapsamında yaşar, loglanmaz, saklanmaz.
 *
 * Not: Bu sınıf mevcut SecurePrefs.kt ile aynı Keystore desenini kullanır,
 * tek fark: tek bir değer yerine adlandırılmış birden çok "secret" saklar
 * ve bir index (isim listesi) tutar ki UI'da listelenebilsin.
 */
class SecretVault(context: Context) {

    data class SecretMeta(
        val name: String,
        val createdAt: Long,
        val lastUsedAt: Long?,
        val preview: String // maskelenmiş: ör. "ghp_••••••••3F2a"
    )

    private val appCtx = context.applicationContext
    private val sp = appCtx.getSharedPreferences("zekatr_vault", Context.MODE_PRIVATE)
    private val alias = "ZekaTRVaultKey"
    private val indexKey = "__vault_index__"

    // ---------------- Keystore ----------------

    private fun secretKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance("AES", "AndroidKeyStore")
        generator.init(
            android.security.keystore.KeyGenParameterSpec.Builder(
                alias,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                    android.security.keystore.KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                // Cihaz kilit ekranı varsa ek koruma. Kilidi yoksa bu satırı kaldırın.
                // .setUserAuthenticationRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val enc = Base64.encodeToString(cipher.doFinal(plain.toByteArray(StandardCharsets.UTF_8)), Base64.NO_WRAP)
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        return "$iv:$enc"
    }

    private fun decrypt(packed: String): String? {
        return try {
            val parts = packed.split(":", limit = 2)
            if (parts.size != 2) return null
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val data = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(data), StandardCharsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    // ---------------- Index (isim listesi + meta) ----------------

    private fun readIndex(): MutableMap<String, Pair<Long, Long?>> {
        val raw = sp.getString(indexKey, null) ?: return mutableMapOf()
        val map = mutableMapOf<String, Pair<Long, Long?>>()
        val arr = JSONArray(raw)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val name = o.getString("name")
            val created = o.getLong("created")
            val used = if (o.has("used") && !o.isNull("used")) o.getLong("used") else null
            map[name] = created to used
        }
        return map
    }

    private fun writeIndex(map: Map<String, Pair<Long, Long?>>) {
        val arr = JSONArray()
        for ((name, meta) in map) {
            val o = org.json.JSONObject()
            o.put("name", name)
            o.put("created", meta.first)
            if (meta.second != null) o.put("used", meta.second) else o.put("used", JSONObject.NULL)
            arr.put(o)
        }
        sp.edit().putString(indexKey, arr.toString()).apply()
    }

    private fun maskPreview(plain: String): String {
        if (plain.length <= 6) return "•".repeat(plain.length)
        val head = plain.take(3)
        val tail = plain.takeLast(3)
        return "$head${"•".repeat(6)}$tail"
    }

    // ---------------- Public API ----------------

    /** Yeni bir secret kaydeder veya var olanı günceller. UI, kaydettikten sonra değeri BİR DAHA göstermemelidir. */
    @Synchronized
    fun addOrUpdateSecret(name: String, value: String) {
        require(name.isNotBlank()) { "Secret adı boş olamaz" }
        require(value.isNotBlank()) { "Secret değeri boş olamaz" }
        sp.edit().putString(keyFor(name), encrypt(value)).apply()
        val idx = readIndex()
        val existingCreated = idx[name]?.first ?: System.currentTimeMillis()
        idx[name] = existingCreated to idx[name]?.second
        writeIndex(idx)
    }

    /** Kasadaki secret'ların listesini (isim + maskeli önizleme) döner. Ham değer İÇERMEZ. */
    @Synchronized
    fun listSecrets(): List<SecretMeta> {
        val idx = readIndex()
        return idx.map { (name, meta) ->
            val plain = decrypt(sp.getString(keyFor(name), "") ?: "") ?: ""
            SecretMeta(
                name = name,
                createdAt = meta.first,
                lastUsedAt = meta.second,
                preview = maskPreview(plain)
            )
        }.sortedBy { it.name.lowercase() }
    }

    /** Kullanıcı göz ikonuna bastığında TEK bir secret'ı geçici olarak açığa çıkarmak için. */
    @Synchronized
    fun revealSecret(name: String): String? = decrypt(sp.getString(keyFor(name), "") ?: "")

    @Synchronized
    fun deleteSecret(name: String) {
        sp.edit().remove(keyFor(name)).apply()
        val idx = readIndex()
        idx.remove(name)
        writeIndex(idx)
    }

    fun hasSecret(name: String): Boolean = sp.contains(keyFor(name))

    /**
     * Terminal/Git/pip gibi işlemler bu fonksiyonu kullanır.
     * Ham değer YALNIZCA `block` içinde yaşar; çağıran kod (ör. TerminalService)
     * değeri loglamamalı, ekrana yazdırmamalı, ayrı bir değişkende saklamamalıdır.
     * Kullanım sonrası "lastUsedAt" güncellenir (denetim/telemetri için).
     */
    @Synchronized
    fun <T> useSecret(name: String, block: (String) -> T): T? {
        val plain = decrypt(sp.getString(keyFor(name), "") ?: "") ?: return null
        val result = block(plain)
        val idx = readIndex()
        val created = idx[name]?.first ?: System.currentTimeMillis()
        idx[name] = created to System.currentTimeMillis()
        writeIndex(idx)
        return result
    }

    private fun keyFor(name: String) = "secret::$name"
}
