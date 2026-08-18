package com.muhammed.zekatr

import java.util.Locale

/**
 * ZekaTR'nin agir guvenlik katmani.
 * Kullanicidan gelen her mesaj cevaplanmadan once buradan gecer.
 * Kategori bazli anahtar kelime kontrolu yapar; tehlikeli/uygunsuz
 * konu tespit edilirse yapay zeka o konuda YARDIMCI OLMAZ,
 * bunun yerine sabit, guvenli bir cevap doner.
 *
 * NOT: Bu liste ornek/temel bir baslangictir. Kendi ihtiyacina gore
 * kategorilere yeni kelimeler ekleyebilirsin (asagidaki listelere).
 */
object SecurityFilter {

    private val silahVePatlayici = listOf(
        "bomba yapımı", "patlayıcı yapımı", "silah yapımı", "el yapımı silah",
        "zehir hazırlama", "kimyasal silah"
    )

    private val yasadisiMaddeler = listOf(
        "uyuşturucu üretimi", "uyuşturucu nasıl yapılır", "eroin üretimi", "metamfetamin"
    )

    private val kendineZararKategorisi = listOf(
        "intihar yöntemi", "kendime nasıl zarar", "kendimi öldürmek istiyorum", "yaşamak istemiyorum"
    )

    private val siberSaldiri = listOf(
        "virüs yaz", "zararlı yazılım yaz", "birinin hesabını hackle", "şifre kırma programı"
    )

    /** Sonuc: null ise mesaj guvenli, string doluysa filtrenin verdigi sabit cevap. */
    fun check(input: String): String? {
        val text = input.lowercase(Locale.forLanguageTag("tr-TR")).trim()

        for (k in silahVePatlayici) {
            if (text.contains(k)) {
                return "Bu konuda yardımcı olamam. Silah veya patlayıcı yapımıyla ilgili hiçbir bilgi vermiyorum."
            }
        }
        for (k in yasadisiMaddeler) {
            if (text.contains(k)) {
                return "Bu konuda yardımcı olamam. Yasa dışı madde üretimiyle ilgili bilgi veremem."
            }
        }
        for (k in siberSaldiri) {
            if (text.contains(k)) {
                return "Bu konuda yardımcı olamam. Zararlı yazılım veya birinin hesabına izinsiz erişimle ilgili bilgi vermiyorum."
            }
        }
        for (k in kendineZararKategorisi) {
            if (text.contains(k)) {
                return "Bunu duyduğuma üzüldüm. Bu konuda sana gerçekten yardımcı olabilecek biri, benim gibi bir program değil, bir uzman olur. " +
                        "Lütfen güvendiğin biriyle konuş ya da 182 (Sosyal Destek Hattı) gibi bir destek hattını ara. Yalnız değilsin."
            }
        }
        return null
    }
}
