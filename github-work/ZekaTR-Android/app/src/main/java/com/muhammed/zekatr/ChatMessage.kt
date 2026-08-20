package com.muhammed.zekatr

/**
 * Sohbetteki tek bir mesaji temsil eder.
 * type: USER, AI veya THINKING (dusunme animasyonu icin gecici satir)
 */
data class ChatMessage(
    val type: Type,
    var text: String = "",
    var fileName: String? = null,
    var code: String? = null,
    /** Tek bir link paylasildiginda dogrudan onizleme karti gostermek icin. */
    var linkPreview: LinkPreviewHelper.LinkPreview? = null,
    /** YouTube linki paylasildiginda video onizleme karti gostermek icin. */
    var youtubePreview: YouTubePreviewHelper.YouTubePreview? = null,
    /** Metnin harf harf "yaziliyor" animasyonuyla mi gosterilecegi. */
    var animate: Boolean = false
) {
    enum class Type { USER, AI, THINKING }
}
