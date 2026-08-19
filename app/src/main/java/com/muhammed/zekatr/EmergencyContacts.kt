package com.muhammed.zekatr

/**
 * Turkiye'deki resmi acil durum / ihbar hatlari.
 * Bu liste SABIT VE STATIKTIR - yapay zeka bu numaralari asla
 * "uydurmaz" ya da tahmin etmez, dogrudan buradan okur.
 * Boylece yanlis bir numaraya yonlendirme riski ortadan kalkar.
 */
object EmergencyContacts {

    data class Entry(val number: String, val name: String, val keywords: List<String>)

    val ALL = listOf(
        Entry("112", "Acil Çağrı Merkezi (Ambulans/Sağlık)", listOf("112", "ambulans", "acil", "sağlık acil", "kalp krizi", "kaza")),
        Entry("155", "Polis İmdat", listOf("155", "polis", "hırsız", "saldırı", "gasp")),
        Entry("156", "Jandarma İmdat", listOf("156", "jandarma")),
        Entry("110", "İtfaiye", listOf("110", "itfaiye", "yangın")),
        Entry("158", "Sahil Güvenlik", listOf("158", "sahil güvenlik", "boğulma", "denizde")),
        Entry("177", "Orman Yangını İhbar", listOf("177", "orman yangını")),
        Entry("183", "Sosyal Destek Hattı (Aile, Kadın, Çocuk, Engelli, Yaşlı)", listOf("183", "aile danışma", "kadına şiddet", "çocuk istismarı", "sosyal destek")),
        Entry("184", "Sağlık Bakanlığı İletişim Merkezi (SABİM)", listOf("184", "sabim")),
        Entry("185", "Su Arıza (belediyeye göre değişebilir)", listOf("185", "su arıza")),
        Entry("186", "Gaz Arıza (belediyeye göre değişebilir)", listOf("186", "gaz arıza", "doğalgaz kaçağı")),
        Entry("187", "Elektrik Arıza (dağıtım şirketine göre değişebilir)", listOf("187", "elektrik arıza", "elektrik kesintisi")),
        Entry("122", "Sosyal Yardımlaşma", listOf("122")),
        Entry("153", "Belediye Çağrı Merkezi (genel)", listOf("153", "belediye")),
        Entry("115", "Alo Trafik", listOf("115", "trafik ihbar")),
        Entry("174", "Zehir Danışma Merkezi (ULUKOM)", listOf("174", "zehirlenme", "zehir danışma")),
        Entry("182", "Alo Gıda Hattı", listOf("182", "gıda şikayet")),
        Entry("144", "Alo Orman", listOf("144"))
    )

    /**
     * Kullanicinin mesaji bir acil durum ihbar numarasi aramasina benziyorsa
     * dogrudan (sabit) numarayi dondurur. Eslesme yoksa null doner.
     */
    fun findFor(normalizedInput: String): Entry? {
        return ALL.firstOrNull { entry -> entry.keywords.any { normalizedInput.contains(it) } }
    }

    fun listAllAsText(): String {
        val sb = StringBuilder("Türkiye'deki resmi acil durum hatları:\n\n")
        ALL.forEach { sb.append("• ${it.number} — ${it.name}\n") }
        sb.append("\nBu numaralar sabit olarak uygulamaya gömülüdür, yapay zeka tarafından tahmin edilmez.")
        return sb.toString()
    }
}
