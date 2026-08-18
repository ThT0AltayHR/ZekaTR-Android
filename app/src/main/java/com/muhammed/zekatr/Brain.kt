package com.muhammed.zekatr

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * ZekaTR'nin sohbet motoru.
 * Gercek bir "yapay zeka modeli" degildir; anahtar kelime eslestirmesiyle
 * calisan, kural tabanli bir sohbet sistemidir. Bilmedigi bir seyle
 * karsilastiginda kullanicidan ogrenip bunu cihazda kalici olarak saklar.
 *
 * v1.1 notlari (duzeltmeler):
 *  - Turkce kucuk harfe cevirme artik Locale("tr","TR") ile yapiliyor.
 *    (Kotlin'in varsayilan .lowercase() fonksiyonu "İ" gibi Turkce'ye
 *    ozgu harflerde hatali sonuc verebiliyordu, bu duzeltildi.)
 *  - Saat/tarih sorulari artik cihazin kendi saatinden GERCEK cevap veriyor
 *    (buna internet gerekmez, tamamen yerel bir islemdir).
 *  - Basit matematik ifadeleri artik gercekten hesaplaniyor (MathHelper).
 *  - Veri seti onceki surume gore ~7 kat genisletildi.
 */
class Brain(context: Context) {

    private val memory = LearnedData(context.applicationContext)
    private val trLocale = Locale.forLanguageTag("tr-TR")

    private var awaitingTeachFor: String? = null

    private fun normalize(text: String): String {
        return text.lowercase(trLocale)
            .replace(Regex("[^a-zçğıöşü0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    // ----------------------------------------------------------------
    // TURKCE KONUSMA VERI SETI
    // Her satir: (anahtar kelimeler) -> (olasi cevaplar)
    // Buraya istedigin kadar yeni satir ekleyebilirsin.
    // ----------------------------------------------------------------
    private val patterns: List<Pair<List<String>, List<String>>> = listOf(

        // --- Selamlasma ---
        listOf("merhaba", "selam", "selamlar", "hey", "sa", "merhabalar") to listOf(
            "Merhaba! Nasıl yardımcı olabilirim?",
            "Selam! Bugün sana nasıl yardımcı olabilirim?",
            "Merhaba, hoş geldin!"
        ),
        listOf("günaydın") to listOf("Günaydın! Güzel bir gün olsun.", "Günaydın, umarım güzel bir gün geçirirsin!"),
        listOf("iyi akşamlar") to listOf("İyi akşamlar!", "Sana da iyi akşamlar."),
        listOf("iyi geceler") to listOf("İyi geceler, güzel uykular!", "İyi geceler, yarın görüşürüz."),
        listOf("nasılsın", "naber", "ne haber", "n aber", "nörüyorsun", "ne yapıyorsun") to listOf(
            "İyiyim, teşekkür ederim! Sen nasılsın?",
            "Gayet iyiyim, sohbet etmeye hazırım. Sen nasılsın?",
            "İyi sayılırım, sen naber?"
        ),
        listOf("iyiyim", "ben de iyiyim", "fena değilim", "idare eder") to listOf(
            "Bunu duyduğuma sevindim!",
            "Ne güzel, sohbete devam edelim."
        ),
        listOf("kötüyüm", "iyi değilim", "moralim bozuk", "kendimi kötü hissediyorum", "üzgünüm") to listOf(
            "Üzüldüm bunu duyduğuma. Konuşmak bazen iyi gelir, istersen anlatabilirsin.",
            "Umarım kısa sürede daha iyi hissedersin. Yanındayım, ne olduğunu anlatmak ister misin?"
        ),

        // --- Kimlik ---
        listOf("adın ne", "ismin ne", "kimsin", "sen kimsin", "adin ne") to listOf(
            "Ben ZekaTR. Muhammed tarafından geliştirildim ve tamamen cihazının içinde, internete bağlanmadan çalışırım."
        ),
        listOf("seni kim yaptı", "seni kim geliştirdi", "seni kim yazdı", "seni kim üretti", "yaratıcın kim") to listOf(
            "Ben Muhammed tarafından geliştirildim."
        ),
        listOf("nesin", "insan mısın", "robot musun", "bot musun") to listOf(
            "Ben bir insan değilim, telefonunun içinde çalışan bir yazılımım."
        ),
        listOf("ne yapabilirsin", "neler yapabilirsin", "yeteneklerin ne", "yardım", "komutlar", "ne biliyorsun") to listOf(
            "Seninle Türkçe sohbet edebilirim, basit matematik işlemleri yapabilirim, saat/tarih söyleyebilirim ve " +
                    "Python'da kod örnekleri yazabilirim. Ayrıca bana yeni bir şey öğretirsen bunu hatırlarım. " +
                    "Örnek: \"python'da bubble sort yaz\" ya da \"12 çarpı 8 kaç eder\" diyebilirsin."
        ),

        // --- Tesekkur / vedalasma ---
        listOf("teşekkür", "sağol", "sağ ol", "eyvallah", "çok sağ ol") to listOf(
            "Rica ederim!", "Ne demek, her zaman!", "Rica ederim, başka bir konuda da yardımcı olabilirim."
        ),
        listOf("görüşürüz", "hoşça kal", "bay bay", "kendine iyi bak", "güle güle") to listOf(
            "Görüşmek üzere!", "Hoşça kal, yine bekleriz!"
        ),

        // --- Zaman (GERCEK cihaz saatinden) ---
        listOf("saat kaç", "saati söyle") to listOf("__REALTIME__"),
        listOf("bugün ayın kaçı", "tarih ne", "bugünün tarihi", "hangi gündeyiz") to listOf("__REALDATE__"),
        listOf("hangi yıldayız", "yıl kaç") to listOf("__REALYEAR__"),

        // --- Kucuk sohbet / duygular ---
        listOf("seni seviyorum") to listOf("Çok naziksin, teşekkür ederim! Ben de sohbet etmekten keyif alıyorum."),
        listOf("senden nefret ediyorum", "seni sevmiyorum") to listOf(
            "Üzgünüm bunu duyduğuma. Daha iyi yardımcı olabilmem için ne bekliyordun, söyler misin?"
        ),
        listOf("aptalsın", "salaksın", "gerizekalısın") to listOf(
            "Kusura bakma seni hayal kırıklığına uğrattıysam. Daha iyi olmam için bana yardımcı olabilirsin."
        ),
        listOf("harikasın", "süpersin", "çok iyisin", "aferin") to listOf(
            "Çok teşekkür ederim, bu beni mutlu etti!"
        ),
        listOf("şaka yap", "espri yap", "beni güldür", "fıkra anlat") to listOf(
            "Neden bilgisayarlar hiç üşümez? Çünkü Windows'ları hep kapalıdır!",
            "Neden yazılımcılar karanlıkta çalışmayı sever? Çünkü ışık bug'ları çeker!",
            "Bir programcı neden yalnız yaşar? Çünkü ilişkilerde 'null pointer exception' almaktan korkar."
        ),
        listOf("motivasyon", "bana tavsiye ver", "moral ver") to listOf(
            "Küçük adımlar bile ilerlemektir. Bugün attığın her adım seni hedefine biraz daha yaklaştırır.",
            "Zorlandığın an, aslında geliştiğin andır. Devam et!"
        ),

        // --- Genel bilgi (sabit/degismeyen gercekler) ---
        listOf("türkiye nin başkenti", "türkiyenin başkenti", "başkent neresi") to listOf("Türkiye'nin başkenti Ankara'dır."),
        listOf("kaç il var", "türkiye de kaç il") to listOf("Türkiye'de 81 il bulunmaktadır."),
        listOf("en büyük şehir", "en kalabalık şehir") to listOf("Nüfus bakımından Türkiye'nin en büyük şehri İstanbul'dur."),
        listOf("bir haftada kaç gün", "haftanın günleri") to listOf("Bir haftada 7 gün vardır: Pazartesi, Salı, Çarşamba, Perşembe, Cuma, Cumartesi, Pazar."),
        listOf("bir yılda kaç ay") to listOf("Bir yılda 12 ay vardır."),
        listOf("dünya kaç yaşında") to listOf("Bilim insanlarına göre Dünya yaklaşık 4.5 milyar yaşındadır."),

        // --- Programlama kavramlari (egitici tanimlar) ---
        listOf("python nedir") to listOf(
            "Python; okunması kolay, sade sözdizimine sahip, web geliştirmeden veri bilimine kadar birçok alanda kullanılan popüler bir programlama dilidir."
        ),
        listOf("değişken nedir") to listOf(
            "Değişken, bir programda veri saklamak için kullanılan, ismiyle çağrılabilen bir kutuya benzer. Örneğin Python'da: yas = 25"
        ),
        listOf("döngü nedir") to listOf(
            "Döngü, bir işlemi belirli bir koşul sağlanana kadar tekrar tekrar çalıştırmaya yarar. Python'da en çok 'for' ve 'while' döngüleri kullanılır."
        ),
        listOf("fonksiyon nedir") to listOf(
            "Fonksiyon, belirli bir işi yapmak üzere yazılmış, tekrar tekrar çağrılabilen kod bloğudur. Python'da 'def' anahtar kelimesiyle tanımlanır."
        ),
        listOf("liste nedir python") to listOf(
            "Python'da liste, birden fazla veriyi sıralı şekilde tutan bir veri yapısıdır. Örnek: sayilar = [1, 2, 3, 4]"
        ),
        listOf("sözlük nedir python", "dictionary nedir") to listOf(
            "Python'da sözlük (dictionary), anahtar-değer çiftleriyle veri tutan bir yapıdır. Örnek: kisi = {\"isim\": \"Ahmet\", \"yas\": 30}"
        ),

        // --- Eglence ---
        listOf("bir renk söyle") to listOf("Mavi", "Kırmızı", "Yeşil", "Mor"),
        listOf("bir sayı söyle", "rastgele sayı") to listOf((1..100).random().toString()),
        listOf("kedi nasıl ses çıkarır") to listOf("Miyav!"),
        listOf("köpek nasıl ses çıkarır") to listOf("Hav hav!"),

        // --- Ogrenilenler hakkinda meta konusma ---
        listOf("kaç şey öğrendin", "ne kadar öğrendin", "hafızanda ne var") to listOf("__LEARNED_COUNT__"),
        listOf("her şeyi unut", "hafızanı sil", "öğrendiklerini sil", "beynini sıfırla") to listOf("__FORGET_ALL__")
    )

    /**
     * Kullanicinin mesajina verilecek cevabi hesaplar.
     * Donen Triple: (cevap metni, dosya adi varsa, kod varsa)
     */
    fun respond(userInput: String): Triple<String, String?, String?> {
        val trimmedInput = userInput.trim()

        // 1) Guvenlik katmani her seyden once calisir
        SecurityFilter.check(trimmedInput)?.let { return Triple(it, null, null) }

        // 2) Ogretme modu bekleniyorsa, bu mesaji cevap olarak kaydet
        awaitingTeachFor?.let { question ->
            awaitingTeachFor = null
            val keywords = normalize(question).split(" ").filter { it.length > 2 }
            if (keywords.isNotEmpty()) {
                keywords.forEach { memory.teach(it, trimmedInput) }
            }
            return Triple("Teşekkürler, bunu öğrendim! Artık bunu hatırlayacağım.", null, null)
        }

        // 3) Kullanici acikca ogretmek istiyorsa: "öğret: soru -> cevap"
        if (trimmedInput.startsWith("öğret:", ignoreCase = true) || trimmedInput.contains("->")) {
            val parts = trimmedInput.substringAfter(":").split("->")
            if (parts.size == 2) {
                val soru = normalize(parts[0])
                val cevap = parts[1].trim()
                val keywords = soru.split(" ").filter { it.length > 2 }
                keywords.forEach { memory.teach(it, cevap) }
                return Triple("Anladım, bunu öğrendim. Bundan sonra bu şekilde cevap vereceğim.", null, null)
            }
        }

        // 4) Kod uretme istegi mi? (Python odakli genis kutuphane)
        CodeGenerator.tryGenerate(trimmedInput)?.let { result ->
            return Triple(result.explanation, result.fileName, result.code)
        }

        // 5) Matematik ifadesi mi?
        MathHelper.tryEvaluate(trimmedInput)?.let { return Triple(it, null, null) }

        val normalized = normalize(trimmedInput)
        val words = normalized.split(" ").filter { it.isNotBlank() }

        // 6) Sabit kaliplarla eslestirme (en cok kelime eslesen kazanir)
        var bestKeywords: List<String>? = null
        var bestScore = 0
        for ((keywords, _) in patterns) {
            val score = keywords.count { normalized.contains(it) }
            if (score > bestScore) {
                bestScore = score
                bestKeywords = keywords
            }
        }
        if (bestKeywords != null && bestScore > 0) {
            val idx = patterns.indexOfFirst { it.first == bestKeywords }
            val response = patterns[idx].second.random()
            return Triple(resolveSpecialToken(response), null, null)
        }

        // 7) Daha once ogrenilmis bir cevap var mi?
        memory.findAnswer(words)?.let { return Triple(it, null, null) }

        // 8) Hicbir sey bulunamadi -> ogrenme moduna gec
        awaitingTeachFor = trimmedInput
        return Triple(
            "Bunu henüz bilmiyorum. Bana ne cevap vermemi istersin? Yazdığın cevabı hatırlayacağım.",
            null,
            null
        )
    }

    /** __REALTIME__ gibi ozel isaretleri gercek deger uretecek fonksiyonlara baglar. */
    private fun resolveSpecialToken(token: String): String {
        val cal = Calendar.getInstance()
        return when (token) {
            "__REALTIME__" -> {
                val fmt = SimpleDateFormat("HH:mm", trLocale)
                "Şu an saat ${fmt.format(cal.time)}."
            }
            "__REALDATE__" -> {
                val fmt = SimpleDateFormat("d MMMM yyyy, EEEE", trLocale)
                "Bugün ${fmt.format(cal.time)}."
            }
            "__REALYEAR__" -> "İçinde bulunduğumuz yıl ${cal.get(Calendar.YEAR)}."
            "__LEARNED_COUNT__" -> "Şu ana kadar hafızama ${memory.learnedCount()} farklı konu/kelime kaydettim."
            "__FORGET_ALL__" -> {
                memory.clearAll()
                "Tamam, sana öğrettiğin her şeyi hafızamdan sildim."
            }
            else -> token
        }
    }

    fun learnedCount(): Int = memory.learnedCount()
    fun forgetEverything() = memory.clearAll()
}
