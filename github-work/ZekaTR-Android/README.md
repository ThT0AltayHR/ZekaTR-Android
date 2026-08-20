# ZekaTR 🧠

ZekaTR, Android üzerinde çalışan, yerel hafıza ile başlayıp seçilebilir AI sağlayıcıları, streaming, web araştırması, dosya/proje analizi, sesli kullanım ve cihaz araçlarına doğru genişleyen kişisel yapay zekâ asistanı projesidir.

## Öne çıkan yetenekler

- 🤖 AI Router: OpenAI, Groq, OpenRouter, Gemini ve Claude desteği
- ⚡ Streaming: OpenAI-uyumlu sağlayıcılarda SSE ile parça parça cevap
- 🧠 Kalıcı yerel hafıza
- 🔎 Web araması ve kaynak önizlemeleri
- 📎 TXT, DOCX, ZIP ve kod dosyası analizi
- 📦 ZIP proje yapısı tarama ve Android/Kotlin/Java/Python dosya keşfi
- 🎙️ Türkçe sesli giriş ve TTS cevap
- 📱 Cihaz araçları: uygulama açma, YouTube arama, rehber üzerinden telefon/WhatsApp/Telegram akışları
- 🌦️ Konum izni verildiğinde hava durumu bildirimleri
- 📰 Son dakika haber bildirimi
- 🎉 Türkiye özel gün/bayram bildirimleri
- 🧠 Eğitim Merkezi: GitHub API üzerinden seçilmiş kodlama ve güvenlik araştırmalarını yerel bilgi paketine işler
- 🔐 API anahtarları Android Keystore tabanlı şifreli depolama ile saklanır
- 🔑 Google Sign-In altyapısı

## Eğitim Merkezi

Eğitim Merkezi, uygulamanın kaynak kodunu uzaktan kendi kendine değiştirmek yerine, güvenli bir bilgi güncelleme akışı kullanır. Kotlin, Java, Python, C++, shell, Android güvenliği, OWASP ve LLM konuları için GitHub araştırması yapar ve seçilmiş bulguları yerel bilgi paketine kaydeder. Bu ayrım, indirilen uzaktan kodun uygulamanın güvenlik katmanını sessizce değiştirmesini önlemek içindir.

## Bildirimler

Android'in arka plan kısıtları nedeniyle bildirim motoru WorkManager ile çalışır. Hava durumu yaklaşık 30 dakikalık sistem kontrollü aralıklarla kontrol edilir ve hava kodu değiştiğinde bildirim oluşturulur. Haber ve özel gün bildirimleri de aynı güvenli arka plan katmanından yönetilir.

## AI sağlayıcı kurulumu

Ayarlar → AI Modeli bölümünden sağlayıcı, model ve API anahtarını seçebilirsin. Anahtarlar kaynak koduna yazılmaz; cihazdaki Android Keystore ile korunur.

## Google giriş

`default_web_client_id` değeri Android kaynaklarında tutulabilir; OAuth istemci kimliği gizli bir parola değildir. Google Cloud tarafında Android uygulamasının paket adı ve SHA-1 imzası doğru yapılandırılmalıdır.

## Release

GitHub Releases kullanıldığında APK dosyası release varlığı olarak yüklenebilir. GitHub, release sayfasında dosya için doğrudan indirme bağlantısı gösterir. İstersen proje CI/CD'sine GitHub Actions ekleyerek her tag işleminde release APK'sı otomatik üretilebilir.

## Lisans ve güvenlik

Bu proje API anahtarlarını repository içine commit etmemek üzere tasarlanmıştır. Uygulama araçları kullanıcı tarafından verilen Android izinleriyle sınırlıdır. Uzaktan gelen içerik hiçbir zaman otomatik olarak uygulama kodu olarak çalıştırılmamalıdır.
