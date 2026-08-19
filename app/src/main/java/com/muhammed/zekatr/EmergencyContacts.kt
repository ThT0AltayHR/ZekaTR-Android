package com.muhammed.zekatr

/** Türkiye'de kullanıcıya gösterilen sabit/resmi hatlar. */
object EmergencyContacts {
    data class Entry(val number: String, val name: String, val keywords: List<String>)
    val ALL = listOf(
        Entry("112", "Tek Numara Acil Çağrı (polis, jandarma, ambulans, itfaiye vb.)", listOf("112", "acil", "ambulans", "polis", "jandarma", "itfaiye", "yangın", "kaza", "gasp")),
        Entry("183", "Alo 183 Sosyal Destek", listOf("183", "sosyal destek", "kadına şiddet", "çocuk istismarı", "aile")),
        Entry("184", "SABİM", listOf("184", "sabim", "sağlık danışma")),
        Entry("174", "Alo 174 Gıda Hattı", listOf("174", "gıda şikayet", "alo gıda")),
        Entry("144", "Alo 144 Sosyal Yardım", listOf("144", "sosyal yardım")),
        Entry("182", "MHRS", listOf("182", "mhrs", "hastane randevu")),
        Entry("175", "Tüketici Danışma Hattı", listOf("175", "tüketici")),
        Entry("153", "Belediye Çağrı Merkezi", listOf("153", "belediye"))
    )
    fun findFor(normalizedInput: String): Entry? = ALL.firstOrNull { it.keywords.any { key -> normalizedInput.contains(key) } }
    fun listAllAsText(): String = buildString {
        append("Türkiye'deki önemli resmi/kurumsal hatlar:\n\n")
        ALL.forEach { append("• ${it.number} — ${it.name}\n") }
        append("\nAcil durumlarda tek numara: 112.")
    }
}
