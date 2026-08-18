package com.muhammed.zekatr

/**
 * Sohbetteki tek bir mesaji temsil eder.
 * type: USER, AI veya THINKING (dusunme animasyonu icin gecici satir)
 */
data class ChatMessage(
    val type: Type,
    var text: String = "",
    var fileName: String? = null,
    var code: String? = null
) {
    enum class Type { USER, AI, THINKING }
}
