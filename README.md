# ZekaTR

**Muhammed tarafından geliştirilen, tamamen cihaz üzerinde (offline) çalışan yerel Türkçe sohbet uygulaması.**

## v1.1 — Bu sürümde neler değişti?

- 🐛 **Hata düzeltmesi:** Türkçe küçük harfe çevirme artık `Locale("tr","TR")` ile yapılıyor. Önceki sürüm Kotlin'in varsayılan `.lowercase()` fonksiyonunu kullanıyordu; bu, "İ" gibi Türkçe'ye özgü harflerde (özellikle büyük İ → küçük i dönüşümünde) hatalı eşleşmelere yol açabiliyordu. Artık düzeltildi.
- 🐛 **Hata düzeltmesi:** "Saat kaç?" gibi sorularda önceki sürüm yanlışlıkla "internete bağlı olmadığım için bilemiyorum" diyordu. Bu **yanlıştı** — cihaz saati okumak internet gerektirmez. Artık `Brain.kt` gerçekten cihazın kendi saatinden **doğru saat/tarih/yıl** bilgisini veriyor.
- ✨ **Yeni:** Basit matematik ifadeleri artık gerçekten hesaplanıyor (`MathHelper.kt`) — "12 çarpı 8 kaç eder", "144'ün karekökü" gibi sorular kalıp ezberi değil, gerçek zamanlı hesaplamayla cevaplanıyor.
- ✨ **Genişletme:** Türkçe sohbet veri seti (`Brain.kt`) önceki sürüme göre ~7 kat büyütüldü: selamlaşma, duygular, genel kültür, programlama kavramları, eğlence, hafıza yönetimi ("her şeyi unut" komutu dahil) gibi çok daha fazla kategori eklendi.
- ✨ **Genişletme:** Python kod kütüphanesi (`CodeGenerator.kt`) artık **60'tan fazla** hazır, doğru çalışan Python örneği içeriyor: sıralama/arama algoritmaları, veri yapıları (yığın, kuyruk, bağlı liste), OOP/sınıflar, dosya işlemleri, hata yönetimi, decorator/generator/lambda, regex, unit test, threading, basit oyunlar ve daha fazlası. Eşleştirme mantığı da artık skor bazlı çalışıyor (en iyi eşleşen şablon seçiliyor), önceki "ilk eşleşeni al" mantığındaki hataları giderdi.

**Önemli ve dürüst bir uyarı:** "Python'da her türlü kodu yazabilsin" isteği için gerçekçi olmam gerekiyor: bu uygulama hâlâ bir **şablon kütüphanesi**, gerçek bir kod-anlayan yapay zeka değil. Kütüphaneyi ne kadar genişletirsen (`CodeGenerator.kt` içine yeni `Template` satırları ekleyerek) o kadar çok isteğe doğru cevap verir, ama tamamen yeni/özgün bir kod isteği geldiğinde (örneğin çok spesifik bir iş mantığı) en yakın örneği verir, gerçekten senin tam istediğin kodu "anlayıp" yazamaz. Aşağıdaki bölüm 5'te yeni şablon eklemeyi anlattım — bu, kapsamı büyütmenin tek gerçekçi yolu.

ZekaTR, hiçbir sunucuya, API'ye veya buluta bağlanmadan; tamamen telefonun kendi işlemcisinde çalışan, Türkçe sohbet edebilen, basit kod örnekleri üretebilen ve kendisine öğretilen yeni bilgileri cihazda kalıcı olarak saklayan bir Android uygulamasıdır.

---

## 1. Bu proje gerçekte ne yapıyor? (Dürüst açıklama)

Bu bölümü atlamadan okumanı öneririm, çünkü ilerde "neden ChatGPT gibi değil?" sorusuna cevap olacak.

ZekaTR; ChatGPT, Claude gibi büyük dil modelleriyle **aynı teknolojiyi kullanmaz.** O modeller milyarlarca parametre, trilyonlarca kelimelik veri ve aylarca süren, milyonlarca dolarlık eğitim süreçleriyle ortaya çıkar. Bunu bireysel olarak, sıfırdan, kaliteli biçimde yeniden üretmek mümkün değildir.

Bunun yerine ZekaTR, **kural tabanlı bir sohbet motoru (pattern matching)** kullanır:

- Kullanıcının yazdığı cümledeki anahtar kelimeleri tanır (`Brain.kt`).
- Önceden tanımlanmış kalıplarla eşleştirip uygun bir cevap seçer.
- Bilmediği bir şeyle karşılaşırsa kullanıcıya sorar: *"Bunu bilmiyorum, bana ne cevap vermemi istersin?"* ve verilen cevabı **cihazın kalıcı deposuna (SharedPreferences)** kaydeder. Bir daha aynı/benzer soru sorulduğunda bunu hatırlar.
- Basit "kod yaz" isteklerinde (`CodeGenerator.kt`), önceden hazırlanmış şablonlardan uygun olanı verir. Bu **gerçek bir kod üretme zekası değildir** — bir şablon kütüphanesidir. Zamanla `CodeGenerator.kt` içindeki `templates` listesine yeni kalıplar ekleyerek genişletebilirsin.

Bu mimarinin avantajı: **%100 offline çalışır, hiçbir dış kaynağa bağlı değildir, uygulama boyutu çok küçüktür (birkaç MB), ve tamamen senin kontrolündedir.**

Dezavantajı: Gerçek bir yapay zeka gibi akıl yürütemez, öğrenmediği bir konuda anlamlı cevap veremez. Ne kadar çok kalıp/örnek eklersen o kadar "akıllı" görünür — ama temelde hâlâ bir eşleştirme sistemidir.

---

## 2. Özellikler

- ✅ Tamamen offline — `AndroidManifest.xml`'de `INTERNET` izni bile yok.
- ✅ Claude benzeri sohbet arayüzü (baloncuklar, düşünme animasyonu, kod bloğu görünümü).
- ✅ Kod isteği geldiğinde "⚙ Oluşturuluyor: dosyaadi.py" animasyonu ile dosya oluşturma hissi.
- ✅ Kullanıcı yeni bir şey öğrettiğinde bunu cihazda kalıcı olarak saklar (uygulama kapansa/telefon yeniden başlasa bile hatırlar).
- ✅ Ağır bir güvenlik/içerik filtresi katmanı (`SecurityFilter.kt`) — silah, patlayıcı, yasa dışı madde, siber saldırı ve kendine zarar verme gibi konularda otomatik olarak güvenli/sabit cevaplar döner.
- ✅ Kimlik sorulduğunda: *"Ben Muhammed tarafından geliştirildim."*

---

## 3. Proje yapısı

```
ZekaTR/
├── app/
│   ├── build.gradle                     → Modül bağımlılıkları
│   └── src/main/
│       ├── AndroidManifest.xml          → İzinler (internet izni YOK)
│       ├── java/com/muhammed/zekatr/
│       │   ├── MainActivity.kt          → Ekranı ve akışı yönetir
│       │   ├── Brain.kt                 → ⭐ Sohbet motoru (kalp burası)
│       │   ├── LearnedData.kt           → Öğrenilenleri cihazda saklar
│       │   ├── CodeGenerator.kt         → 60+ örnekli Python kod kütüphanesi
│       │   ├── MathHelper.kt            → Gerçek zamanlı matematik hesaplayıcı
│       │   ├── SecurityFilter.kt        → Güvenlik/içerik filtresi
│       │   ├── ChatAdapter.kt           → RecyclerView adaptörü
│       │   └── ChatMessage.kt           → Mesaj veri modeli
│       └── res/                         → Arayüz (layout, renkler, ikonlar)
├── .github/workflows/build-apk.yml      → GitHub Actions APK derleme
├── build.gradle / settings.gradle       → Proje ayarları
└── README.md                            → Bu dosya
```

---

## 4. Nasıl APK haline getirilir?

### Yöntem A — GitHub Actions (önerilen, bilgisayarına hiçbir şey kurman gerekmez)

1. Bu proje klasörünü kendi GitHub reponun köküne yükle (push et).
2. `.github/workflows/build-apk.yml` otomatik olarak devreye girer.
3. GitHub reponda **Actions** sekmesine git → çalışan iş akışını aç → sonunda **Artifacts** bölümünden `ZekaTR-debug-apk` dosyasını indir.
4. İçinden çıkan `app-debug.apk` dosyasını telefonuna atıp kur (bilinmeyen kaynaklara izin vermen gerekebilir).

Bu workflow, Gradle wrapper jar dosyasına **bağımlı değildir** — `gradle/actions/setup-gradle` aksiyonu Gradle 8.4'ü doğrudan kurup çalıştırır. Bu, önceki "gradle wrapper hatası" tarzı sorunları ortadan kaldırır.

### Yöntem B — Android Studio ile yerel derleme

1. Android Studio'yu aç → **Open** → bu klasörü seç.
2. Gradle senkronizasyonunun bitmesini bekle (internet gerekir, sadece bağımlılıkları indirmek için).
3. `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`.

---

## 5. Sohbet motorunu nasıl genişletirsin?

### Yeni bir kalıp/cevap eklemek
`Brain.kt` içindeki `patterns` listesine yeni bir satır ekle:

```kotlin
listOf("hava nasıl", "bugün hava") to listOf(
    "Ben internete bağlı olmadığım için hava durumunu bilemiyorum, ama umarım güzeldir!"
),
```

### Yeni bir kod şablonu eklemek
`CodeGenerator.kt` içindeki `templates` listesine yeni bir `Triple` ekle: (anahtar kelimeler, açıklama, dosya adı + kod).

### Güvenlik filtresine yeni kelime eklemek
`SecurityFilter.kt` içindeki ilgili kategori listesine (örn. `silahVePatlayici`) yeni kelimeler ekleyebilirsin.

---

## 6. Sınırlamalar (dürüstçe)

- Gerçek bir dil modeli değildir; karmaşık, çok anlamlı veya bağlam gerektiren cümlelerde zorlanır.
- Kod üretimi yalnızca önceden tanımlanmış şablonlarla sınırlıdır.
- Ses ile sohbet ve görsel üretimi bu sürümde **yoktur** — bunlar gerçek yapay zeka modelleri (konuşma tanıma, TTS, diffusion tabanlı görsel üretimi) gerektirir ve cihaz-içi, sunucusuz biçimde makul bir uygulama boyutuyla gerçekleştirilemez. İstersen ileride bunun yerine küçültülmüş açık kaynaklı modelleri (örn. mobil için optimize edilmiş bir TTS motoru) entegre etmeyi konuşabiliriz — bu tamamen ayrı bir proje aşaması olur.

---

## 7. Gizlilik

Uygulama hiçbir veriyi cihaz dışına göndermez. Tüm "öğrenilen" veriler yalnızca `SharedPreferences` içinde, telefonun kendi deposunda saklanır. Uygulama silindiğinde bu veriler de silinir.

---

**Geliştirici:** Muhammed
**Sürüm:** 1.0
