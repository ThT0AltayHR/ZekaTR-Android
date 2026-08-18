package com.muhammed.zekatr

import java.util.Locale
import kotlin.math.pow

/**
 * "12 artı 7 kaç eder", "8 çarpı 9", "144'ün karekökü" gibi basit
 * Türkçe matematik ifadelerini gerçek zamanlı olarak hesaplar.
 * Bu, ZekaTR'nin kalıp ezberlemeden calisan tek gercek "dinamik" yetenegidir.
 */
object MathHelper {

    private val opWords = mapOf(
        "artı" to '+', "eksi" to '-', "çarpı" to '*', "kere" to '*',
        "bölü" to '/', "üssü" to '^', "üzeri" to '^'
    )

    fun tryEvaluate(inputRaw: String): String? {
        val input = inputRaw.lowercase(Locale.forLanguageTag("tr-TR"))

        // "144'ün karekökü" / "karekök 144"
        Regex("karekök[üu]?\\s*([0-9]+)|([0-9]+).{0,3}karekök").find(input)?.let { m ->
            val numStr = m.groupValues[1].ifBlank { m.groupValues[2] }
            val num = numStr.toDoubleOrNull() ?: return null
            if (num < 0) return "Negatif bir sayının gerçek karekökü yoktur."
            return "${formatNum(num)} sayısının karekökü yaklaşık ${formatNum(Math.sqrt(num))}."
        }

        // Kelimeleri sembole cevir: "12 artı 7" -> "12 + 7"
        var normalized = input
        for ((word, symbol) in opWords) {
            normalized = normalized.replace(word, " $symbol ")
        }

        // Sadece sayi, operator ve bosluk kalan kismi cikar
        val matches = Regex("(-?\\d+(?:[.,]\\d+)?)\\s*([+\\-*/^])\\s*(-?\\d+(?:[.,]\\d+)?)").find(normalized)
            ?: return null

        val a = matches.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        val op = matches.groupValues[2][0]
        val b = matches.groupValues[3].replace(',', '.').toDoubleOrNull() ?: return null

        val result = when (op) {
            '+' -> a + b
            '-' -> a - b
            '*' -> a * b
            '/' -> if (b == 0.0) return "Bir sayı sıfıra bölünemez." else a / b
            '^' -> a.pow(b)
            else -> return null
        }
        return "${formatNum(a)} ${opSymbolToWord(op)} ${formatNum(b)} = ${formatNum(result)}"
    }

    private fun opSymbolToWord(op: Char) = when (op) {
        '+' -> "artı"; '-' -> "eksi"; '*' -> "çarpı"; '/' -> "bölü"; '^' -> "üssü"; else -> "?"
    }

    private fun formatNum(n: Double): String {
        return if (n == n.toLong().toDouble()) n.toLong().toString()
        else String.format(Locale.forLanguageTag("tr-TR"), "%.4f", n).trimEnd('0').trimEnd(',')
    }
}
