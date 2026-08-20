package com.muhammed.zekatr

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import com.muhammed.zekatr.databinding.ActivityYoutubePlayerBinding

/**
 * Onemli/durust not:
 * Bu ekran, YouTube'un SES AKISINI ayiklayip kendi arka plan
 * calarimizda calistirmaz - bu, YouTube Kullanim Sartlari'na aykiridir
 * ve telif hakli muzigin izinsiz cogaltilmasi/dagitimi anlamina gelir,
 * bunu bilerek yapmiyoruz.
 *
 * Bunun yerine YouTube'un KENDI resmi IFrame Player API'sini bir
 * WebView icinde gosteriyoruz (reklamlar dahil, tamamen ToS uyumlu).
 * Bu yontemin dogal sinirlamasi: ekran kapaninca video/ses de durur
 * (WebView render motoru arka planda calismaz). Gercek "ekran kapaliyken
 * YouTube muzigi calma" ozelligi ancak YouTube'un kendi resmi
 * uygulamasi/YouTube Music (Premium) uzerinden, bizim degil YouTube'un
 * kendi altyapisiyla mumkun olur.
 */
class YouTubePlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityYoutubePlayerBinding

    companion object {
        const val EXTRA_VIDEO_ID = "video_id"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityYoutubePlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBackYoutube.setOnClickListener { finish() }

        val videoId = intent.getStringExtra(EXTRA_VIDEO_ID)
        if (videoId.isNullOrBlank()) { finish(); return }

        binding.webYoutubePlayer.settings.apply {
            javaScriptEnabled = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        binding.webYoutubePlayer.webChromeClient = WebChromeClient()

        val html = """
            <html><body style="margin:0;background:#000">
            <div id="player"></div>
            <script src="https://www.youtube.com/iframe_api"></script>
            <script>
              var player;
              function onYouTubeIframeAPIReady() {
                player = new YT.Player('player', {
                  height: '220', width: '100%',
                  videoId: '${videoId.replace("'", "")}',
                  playerVars: { autoplay: 1, playsinline: 1 }
                });
              }
            </script>
            </body></html>
        """.trimIndent()
        binding.webYoutubePlayer.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "utf-8", null)
    }

    override fun onDestroy() {
        binding.webYoutubePlayer.destroy()
        super.onDestroy()
    }
}
