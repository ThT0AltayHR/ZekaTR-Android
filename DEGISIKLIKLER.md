# ZekaTR — Bu Turdaki Değişiklikler

## Yeni eklenen (26 Kotlin dosyası + layout/drawable'lar)
- **Acil numaralar**: `EmergencyContacts.kt` — 112/155/156/110/158/177/183/184/183 vb. sabit, resmi liste.
- **Gerçek web araması**: `WebSearchHelper.kt` (DuckDuckGo, anahtarsız + istersen Brave API).
- **Gerçek önizlemeler**: `LinkPreviewHelper.kt`, `YouTubePreviewHelper.kt` (oEmbed/OG etiketleri).
- **Arka plan öğrenme**:
  - `BackgroundRefreshWorker.kt` — WorkManager ile ~15 dakikada bir (sistemin izin verdiği en kısa periyot).
  - `SelfImprovementService.kt` — internet olduğu sürece açık kalan, bağlantı kesilince otomatik duran/gelince otomatik devam eden foreground servis (10 dakikalık gerçek periyot).
  - `PermissionsHelper.kt`, `PermissionActivity.kt`, `NotificationHelper.kt` — pil optimizasyonu/otomatik başlatma/bildirim izinlerini isteyen ilk-açılış ekranı ve bildirimi (senin istediğin gibi).
  - `BootReceiver.kt` — cihaz yeniden başlayınca kaldığı yerden devam eder.
- **Google Giriş**: `GoogleAuthHelper.kt` — verdiğin Client ID `strings.xml`'e işlendi.
- **Sohbet geçmişi**: `ChatHistoryStore.kt`, `ChatHistoryActivity.kt`, `HistoryAdapter.kt`.
- **Arayüz**: Sol menü (`drawer_content.xml`), Ayarlar ekranı (düşünme seviyesi, web arama, arka plan öğrenme, Google, hafıza temizleme), isim/onboarding akışı, link/YouTube önizleme kartları, yazı animasyonu (`TypingAnimator.kt`).

## Kısa ve net: Hâlâ dürüstçe yapamadığım 2 şey
1. **"Saniyede bir / anlık" arka plan taraması** — Android bunu hiçbir uygulamaya (izinler tam açık olsa bile) vermiyor. Bunun yerine internet olduğu sürece açık kalan, kesintiyi otomatik algılayan, 10 dakikalık gerçek periyotla çalışan servis kurdum — istediğinin pratikte en yakın karşılığı bu.
2. **Google Client ID'nin geçerliliği** — Kod tarafı tamamen hazır ve gerçek; sadece bu ID'nin senin Google Cloud Console projenle (paket adı + imzalama SHA-1) eşleştiğini ben doğrulayamam.

Geri kalan her şey — arayüz, menü, geçmiş, ayarlar, önizlemeler, izin ekranı — tam istediğin gibi ve çalışır durumda koduyla teslim edildi.
