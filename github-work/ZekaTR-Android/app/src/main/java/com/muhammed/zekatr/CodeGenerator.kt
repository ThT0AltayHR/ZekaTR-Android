package com.muhammed.zekatr

import java.util.Locale

/**
 * ZekaTR'nin kod yazma motoru.
 *
 * DURUST NOT: Bu, gercek anlamda "her turlu Python kodunu yazabilen"
 * bir yapay zeka DEGILDIR. Once bunu acikca soylemek gerekir: Boyle bir
 * sey, dil modelini anlayan gercek bir sinir agi ister (GPT/Claude gibi),
 * sablonla taklit edilemez. Bunun yerine burada yapilan sey: onceden
 * yazilmis, dogru calisan, genis bir Python ornek kutuphanesi olusturup
 * kullanicinin istegini en yakin ornekle eslestirmek.
 *
 * Once kullanicinin ifadesindeki anahtar kelimeleri tarar, en yuksek
 * skoru alan sablonu dondurur (Brain.kt'deki mantigin aynisi).
 * templates listesine YENI SATIRLAR ekleyerek kutuphaneyi
 * dilendigin kadar genisletebilirsin - bu, "kapsami" buyutmenin
 * tek gercekci yoludur.
 */
object CodeGenerator {

    data class CodeResult(val fileName: String, val explanation: String, val code: String)

    data class Template(val triggers: List<String>, val explanation: String, val fileName: String, val code: String)

    private val trLocale = Locale.forLanguageTag("tr-TR")

    private val templates: List<Template> = listOf(

        // ---------------- TEMEL ----------------
        Template(
            listOf("merhaba dünya", "hello world", "ilk program"),
            "Python ile klasik 'Merhaba Dünya' programı:",
            "merhaba.py",
            "print(\"Merhaba, Dünya!\")"
        ),
        Template(
            listOf("iki sayı topla", "toplama fonksiyonu", "toplama yaz"),
            "İki sayıyı toplayan fonksiyon:",
            "toplama.py",
            "def topla(a, b):\n    return a + b\n\nprint(topla(5, 7))"
        ),
        Template(
            listOf("çıkarma fonksiyonu", "iki sayı çıkar"),
            "İki sayıyı çıkaran fonksiyon:",
            "cikarma.py",
            "def cikar(a, b):\n    return a - b\n\nprint(cikar(10, 3))"
        ),
        Template(
            listOf("çarpma fonksiyonu", "iki sayı çarp"),
            "İki sayıyı çarpan fonksiyon:",
            "carpma.py",
            "def carp(a, b):\n    return a * b\n\nprint(carp(4, 6))"
        ),
        Template(
            listOf("bölme fonksiyonu", "iki sayı böl"),
            "İki sayıyı bölen, sıfıra bölmeyi kontrol eden fonksiyon:",
            "bolme.py",
            "def bol(a, b):\n    if b == 0:\n        return \"Sıfıra bölünemez\"\n    return a / b\n\nprint(bol(10, 2))"
        ),
        Template(
            listOf("while döngüsü", "while örneği"),
            "1'den 10'a kadar sayan bir while döngüsü:",
            "while_ornek.py",
            "sayac = 1\nwhile sayac <= 10:\n    print(sayac)\n    sayac += 1"
        ),
        Template(
            listOf("for döngüsü", "for örneği"),
            "Bir liste üzerinde gezen for döngüsü:",
            "for_ornek.py",
            "meyveler = [\"elma\", \"armut\", \"muz\"]\nfor meyve in meyveler:\n    print(meyve)"
        ),
        Template(
            listOf("if else", "koşul örneği", "if elif"),
            "Bir sayının pozitif, negatif ya da sıfır olduğunu kontrol eden örnek:",
            "kosul.py",
            "sayi = -5\nif sayi > 0:\n    print(\"Pozitif\")\nelif sayi < 0:\n    print(\"Negatif\")\nelse:\n    print(\"Sıfır\")"
        ),

        // ---------------- SAYI / MATEMATIK ----------------
        Template(
            listOf("faktöriyel"),
            "Faktöriyel hesaplayan iki yöntem (özyinelemeli ve döngülü):",
            "faktoriyel.py",
            "def faktoriyel_ozyineleme(n):\n    if n <= 1:\n        return 1\n    return n * faktoriyel_ozyineleme(n - 1)\n\ndef faktoriyel_donguyle(n):\n    sonuc = 1\n    for i in range(2, n + 1):\n        sonuc *= i\n    return sonuc\n\nprint(faktoriyel_ozyineleme(5))\nprint(faktoriyel_donguyle(5))"
        ),
        Template(
            listOf("fibonacci"),
            "Fibonacci serisini üreten fonksiyon:",
            "fibonacci.py",
            "def fibonacci(n):\n    seri = [0, 1]\n    for i in range(2, n):\n        seri.append(seri[-1] + seri[-2])\n    return seri[:n]\n\nprint(fibonacci(10))"
        ),
        Template(
            listOf("asal sayı kontrolü", "asal mı"),
            "Bir sayının asal olup olmadığını kontrol eden fonksiyon:",
            "asal.py",
            "def asal_mi(n):\n    if n < 2:\n        return False\n    for i in range(2, int(n ** 0.5) + 1):\n        if n % i == 0:\n            return False\n    return True\n\nprint(asal_mi(17))"
        ),
        Template(
            listOf("asal sayıları listele", "eratosthenes"),
            "Belirli bir sayıya kadar tüm asal sayıları bulan 'Eratosthenes Eleği' algoritması:",
            "asal_elek.py",
            "def asallari_bul(limit):\n    elek = [True] * (limit + 1)\n    elek[0] = elek[1] = False\n    for i in range(2, int(limit ** 0.5) + 1):\n        if elek[i]:\n            for j in range(i * i, limit + 1, i):\n                elek[j] = False\n    return [i for i, asal in enumerate(elek) if asal]\n\nprint(asallari_bul(50))"
        ),
        Template(
            listOf("ebob", "en büyük ortak bölen"),
            "İki sayının EBOB'unu (en büyük ortak bölen) bulan fonksiyon:",
            "ebob.py",
            "def ebob(a, b):\n    while b:\n        a, b = b, a % b\n    return a\n\nprint(ebob(48, 18))"
        ),
        Template(
            listOf("ekok", "en küçük ortak kat"),
            "İki sayının EKOK'unu (en küçük ortak kat) bulan fonksiyon:",
            "ekok.py",
            "def ebob(a, b):\n    while b:\n        a, b = b, a % b\n    return a\n\ndef ekok(a, b):\n    return a * b // ebob(a, b)\n\nprint(ekok(4, 6))"
        ),
        Template(
            listOf("basamak toplamı", "rakamları topla"),
            "Bir sayının basamaklarının toplamını bulan fonksiyon:",
            "basamak_toplami.py",
            "def basamak_toplami(n):\n    toplam = 0\n    n = abs(n)\n    while n > 0:\n        toplam += n % 10\n        n //= 10\n    return toplam\n\nprint(basamak_toplami(12345))"
        ),
        Template(
            listOf("sıcaklık dönüştür", "celsius fahrenheit"),
            "Celsius'u Fahrenheit'a çeviren fonksiyon:",
            "sicaklik.py",
            "def celsius_to_fahrenheit(c):\n    return (c * 9 / 5) + 32\n\nprint(celsius_to_fahrenheit(25))"
        ),

        // ---------------- STRING ----------------
        Template(
            listOf("string ters çevir", "kelimeyi ters çevir", "yazıyı ters çevir"),
            "Bir metni ters çeviren örnek:",
            "ters_cevir.py",
            "def ters_cevir(metin):\n    return metin[::-1]\n\nprint(ters_cevir(\"Python\"))"
        ),
        Template(
            listOf("palindrom"),
            "Bir kelimenin palindrom (tersten okunuşu aynı) olup olmadığını kontrol eden fonksiyon:",
            "palindrom.py",
            "def palindrom_mu(kelime):\n    kelime = kelime.lower().replace(\" \", \"\")\n    return kelime == kelime[::-1]\n\nprint(palindrom_mu(\"anna\"))"
        ),
        Template(
            listOf("anagram"),
            "İki kelimenin birbirinin anagramı olup olmadığını kontrol eden fonksiyon:",
            "anagram.py",
            "def anagram_mi(k1, k2):\n    return sorted(k1.lower()) == sorted(k2.lower())\n\nprint(anagram_mi(\"kalem\", \"lekam\"))"
        ),
        Template(
            listOf("sesli harf say", "ünlü harf say"),
            "Bir metindeki sesli harfleri sayan fonksiyon:",
            "sesli_say.py",
            "def sesli_say(metin):\n    sesliler = \"aeıioöuü\"\n    return sum(1 for harf in metin.lower() if harf in sesliler)\n\nprint(sesli_say(\"merhaba dünya\"))"
        ),
        Template(
            listOf("kelime say", "kaç kelime"),
            "Bir cümledeki kelime sayısını bulan örnek:",
            "kelime_say.py",
            "def kelime_sayisi(cumle):\n    return len(cumle.split())\n\nprint(kelime_sayisi(\"Bu bir örnek cümledir\"))"
        ),

        // ---------------- LISTE / SIRALAMA / ARAMA ----------------
        Template(
            listOf("liste sırala", "listeyi sırala"),
            "Python'ın hazır fonksiyonuyla liste sıralama:",
            "sirala.py",
            "sayilar = [5, 2, 9, 1, 7]\nsayilar.sort()\nprint(sayilar)"
        ),
        Template(
            listOf("bubble sort", "kabarcık sıralama"),
            "Kabarcık sıralama (bubble sort) algoritmasının elle yazılmış hali:",
            "bubble_sort.py",
            "def bubble_sort(liste):\n    n = len(liste)\n    for i in range(n):\n        for j in range(0, n - i - 1):\n            if liste[j] > liste[j + 1]:\n                liste[j], liste[j + 1] = liste[j + 1], liste[j]\n    return liste\n\nprint(bubble_sort([5, 2, 9, 1, 7]))"
        ),
        Template(
            listOf("selection sort", "seçmeli sıralama"),
            "Seçmeli sıralama (selection sort) algoritması:",
            "selection_sort.py",
            "def selection_sort(liste):\n    n = len(liste)\n    for i in range(n):\n        min_idx = i\n        for j in range(i + 1, n):\n            if liste[j] < liste[min_idx]:\n                min_idx = j\n        liste[i], liste[min_idx] = liste[min_idx], liste[i]\n    return liste\n\nprint(selection_sort([5, 2, 9, 1, 7]))"
        ),
        Template(
            listOf("insertion sort", "eklemeli sıralama"),
            "Eklemeli sıralama (insertion sort) algoritması:",
            "insertion_sort.py",
            "def insertion_sort(liste):\n    for i in range(1, len(liste)):\n        anahtar = liste[i]\n        j = i - 1\n        while j >= 0 and liste[j] > anahtar:\n            liste[j + 1] = liste[j]\n            j -= 1\n        liste[j + 1] = anahtar\n    return liste\n\nprint(insertion_sort([5, 2, 9, 1, 7]))"
        ),
        Template(
            listOf("quick sort", "hızlı sıralama"),
            "Hızlı sıralama (quick sort) algoritması:",
            "quick_sort.py",
            "def quick_sort(liste):\n    if len(liste) <= 1:\n        return liste\n    pivot = liste[len(liste) // 2]\n    kucukler = [x for x in liste if x < pivot]\n    esitler = [x for x in liste if x == pivot]\n    buyukler = [x for x in liste if x > pivot]\n    return quick_sort(kucukler) + esitler + quick_sort(buyukler)\n\nprint(quick_sort([5, 2, 9, 1, 7]))"
        ),
        Template(
            listOf("binary search", "ikili arama"),
            "Sıralı bir listede ikili arama (binary search) algoritması:",
            "binary_search.py",
            "def binary_search(liste, hedef):\n    sol, sag = 0, len(liste) - 1\n    while sol <= sag:\n        orta = (sol + sag) // 2\n        if liste[orta] == hedef:\n            return orta\n        elif liste[orta] < hedef:\n            sol = orta + 1\n        else:\n            sag = orta - 1\n    return -1\n\nprint(binary_search([1, 3, 5, 7, 9, 11], 7))"
        ),
        Template(
            listOf("linear search", "doğrusal arama", "liste içinde ara"),
            "Bir listede eleman arayan doğrusal arama (linear search):",
            "linear_search.py",
            "def linear_search(liste, hedef):\n    for i, deger in enumerate(liste):\n        if deger == hedef:\n            return i\n    return -1\n\nprint(linear_search([4, 2, 7, 9], 7))"
        ),
        Template(
            listOf("liste ortalama", "ortalamasını bul", "ortalama hesapla"),
            "Bir listenin ortalamasını hesaplayan fonksiyon:",
            "ortalama.py",
            "def ortalama(liste):\n    return sum(liste) / len(liste)\n\nprint(ortalama([10, 20, 30, 40]))"
        ),
        Template(
            listOf("liste maksimum", "en büyük eleman", "en küçük eleman"),
            "Bir listedeki en büyük ve en küçük elemanı bulma:",
            "min_max.py",
            "sayilar = [4, 8, 15, 16, 23, 42]\nprint(\"En büyük:\", max(sayilar))\nprint(\"En küçük:\", min(sayilar))"
        ),
        Template(
            listOf("list comprehension"),
            "List comprehension kullanarak 1-10 arası sayıların karelerini oluşturma:",
            "list_comprehension.py",
            "kareler = [x ** 2 for x in range(1, 11)]\nprint(kareler)"
        ),
        Template(
            listOf("enumerate"),
            "enumerate() kullanarak liste elemanlarını indeksiyle birlikte yazdırma:",
            "enumerate_ornek.py",
            "meyveler = [\"elma\", \"armut\", \"muz\"]\nfor index, meyve in enumerate(meyveler):\n    print(index, meyve)"
        ),
        Template(
            listOf("zip kullanımı", "iki listeyi birleştir"),
            "zip() ile iki listeyi eşleştirerek birlikte kullanma:",
            "zip_ornek.py",
            "isimler = [\"Ali\", \"Ayşe\", \"Mehmet\"]\nyaslar = [25, 30, 22]\nfor isim, yas in zip(isimler, yaslar):\n    print(isim, yas)"
        ),
        Template(
            listOf("map filter", "map ve filter"),
            "map() ve filter() kullanım örneği:",
            "map_filter.py",
            "sayilar = [1, 2, 3, 4, 5, 6]\nkareler = list(map(lambda x: x ** 2, sayilar))\ncift_sayilar = list(filter(lambda x: x % 2 == 0, sayilar))\nprint(kareler)\nprint(cift_sayilar)"
        ),

        // ---------------- SOZLUK / VERI YAPILARI ----------------
        Template(
            listOf("sözlük örneği", "dictionary örneği"),
            "Python sözlük (dictionary) kullanım örneği:",
            "sozluk.py",
            "kisi = {\"isim\": \"Ahmet\", \"yas\": 30, \"sehir\": \"Ankara\"}\nfor anahtar, deger in kisi.items():\n    print(anahtar, \":\", deger)"
        ),
        Template(
            listOf("yığın", "stack veri yapısı"),
            "Liste kullanarak basit bir Yığın (Stack) veri yapısı:",
            "yigin.py",
            "class Yigin:\n    def __init__(self):\n        self.veriler = []\n\n    def push(self, deger):\n        self.veriler.append(deger)\n\n    def pop(self):\n        if not self.veriler:\n            return None\n        return self.veriler.pop()\n\n    def tepe(self):\n        return self.veriler[-1] if self.veriler else None\n\ny = Yigin()\ny.push(1)\ny.push(2)\nprint(y.pop())"
        ),
        Template(
            listOf("kuyruk", "queue veri yapısı"),
            "collections.deque kullanarak basit bir Kuyruk (Queue) veri yapısı:",
            "kuyruk.py",
            "from collections import deque\n\nkuyruk = deque()\nkuyruk.append(1)\nkuyruk.append(2)\nkuyruk.append(3)\nprint(kuyruk.popleft())"
        ),
        Template(
            listOf("bağlı liste", "linked list"),
            "Basit bir tek yönlü Bağlı Liste (Linked List) örneği:",
            "bagli_liste.py",
            "class Node:\n    def __init__(self, deger):\n        self.deger = deger\n        self.sonraki = None\n\nclass BagliListe:\n    def __init__(self):\n        self.bas = None\n\n    def ekle(self, deger):\n        yeni = Node(deger)\n        if not self.bas:\n            self.bas = yeni\n            return\n        gecici = self.bas\n        while gecici.sonraki:\n            gecici = gecici.sonraki\n        gecici.sonraki = yeni\n\n    def yazdir(self):\n        gecici = self.bas\n        while gecici:\n            print(gecici.deger, end=\" -> \")\n            gecici = gecici.sonraki\n        print(\"None\")\n\nliste = BagliListe()\nliste.ekle(1)\nliste.ekle(2)\nliste.yazdir()"
        ),
        Template(
            listOf("matris toplama"),
            "İki matrisi toplayan örnek:",
            "matris_toplama.py",
            "A = [[1, 2], [3, 4]]\nB = [[5, 6], [7, 8]]\nsonuc = [[A[i][j] + B[i][j] for j in range(len(A[0]))] for i in range(len(A))]\nprint(sonuc)"
        ),
        Template(
            listOf("matris çarpımı"),
            "İki matrisi çarpan örnek:",
            "matris_carpma.py",
            "A = [[1, 2], [3, 4]]\nB = [[5, 6], [7, 8]]\nsonuc = [[sum(a * b for a, b in zip(satir, sutun)) for sutun in zip(*B)] for satir in A]\nprint(sonuc)"
        ),

        // ---------------- SINIF / OOP ----------------
        Template(
            listOf("sınıf örneği", "class örneği", "araba sınıfı"),
            "Basit bir Araba sınıfı (class) örneği:",
            "araba.py",
            "class Araba:\n    def __init__(self, marka, model):\n        self.marka = marka\n        self.model = model\n\n    def bilgi_ver(self):\n        return f\"{self.marka} {self.model}\"\n\naraba1 = Araba(\"Toyota\", \"Corolla\")\nprint(araba1.bilgi_ver())"
        ),
        Template(
            listOf("kalıtım", "inheritance"),
            "Kalıtım (inheritance) örneği: Hayvan sınıfından türeyen Kedi sınıfı:",
            "kalitim.py",
            "class Hayvan:\n    def __init__(self, isim):\n        self.isim = isim\n\n    def ses_cikar(self):\n        return \"...\"\n\nclass Kedi(Hayvan):\n    def ses_cikar(self):\n        return \"Miyav!\"\n\nkedi = Kedi(\"Pamuk\")\nprint(kedi.isim, kedi.ses_cikar())"
        ),
        Template(
            listOf("özel exception", "custom exception", "kendi hatanı"),
            "Özel bir hata (exception) sınıfı tanımlama örneği:",
            "ozel_hata.py",
            "class YetersizBakiyeHatasi(Exception):\n    pass\n\ndef para_cek(bakiye, miktar):\n    if miktar > bakiye:\n        raise YetersizBakiyeHatasi(\"Bakiye yetersiz!\")\n    return bakiye - miktar\n\ntry:\n    para_cek(100, 150)\nexcept YetersizBakiyeHatasi as e:\n    print(\"Hata:\", e)"
        ),

        // ---------------- DOSYA / VERI ----------------
        Template(
            listOf("dosyaya yaz", "dosya yazma"),
            "Bir metin dosyasına yazma örneği:",
            "dosya_yaz.py",
            "with open(\"veri.txt\", \"w\", encoding=\"utf-8\") as f:\n    f.write(\"Merhaba, bu dosyaya yazılan bir örnek metindir.\")"
        ),
        Template(
            listOf("dosyadan oku", "dosya okuma"),
            "Bir metin dosyasını okuma örneği:",
            "dosya_oku.py",
            "with open(\"veri.txt\", \"r\", encoding=\"utf-8\") as f:\n    icerik = f.read()\n    print(icerik)"
        ),
        Template(
            listOf("csv oku", "csv dosyası"),
            "csv modülüyle bir CSV dosyasını okuma:",
            "csv_oku.py",
            "import csv\n\nwith open(\"veri.csv\", \"r\", encoding=\"utf-8\") as f:\n    okuyucu = csv.reader(f)\n    for satir in okuyucu:\n        print(satir)"
        ),
        Template(
            listOf("json oku", "json yaz", "json örneği"),
            "json modülüyle veri okuma ve yazma:",
            "json_ornek.py",
            "import json\n\nveri = {\"isim\": \"Ahmet\", \"yas\": 30}\n\nwith open(\"veri.json\", \"w\", encoding=\"utf-8\") as f:\n    json.dump(veri, f, ensure_ascii=False)\n\nwith open(\"veri.json\", \"r\", encoding=\"utf-8\") as f:\n    okunan = json.load(f)\n    print(okunan)"
        ),

        // ---------------- HATA YONETIMI ----------------
        Template(
            listOf("try except", "hata yönetimi"),
            "try/except ile hata yönetimi örneği:",
            "hata_yonetimi.py",
            "try:\n    sayi = int(input(\"Bir sayı gir: \"))\n    print(10 / sayi)\nexcept ValueError:\n    print(\"Geçerli bir sayı girmedin!\")\nexcept ZeroDivisionError:\n    print(\"Sıfıra bölemezsin!\")\nfinally:\n    print(\"İşlem tamamlandı.\")"
        ),

        // ---------------- ILERI SEVIYE ----------------
        Template(
            listOf("lambda fonksiyon", "lambda örneği"),
            "Lambda (anonim) fonksiyon örneği:",
            "lambda_ornek.py",
            "kare_al = lambda x: x ** 2\nprint(kare_al(6))"
        ),
        Template(
            listOf("generator", "yield örneği"),
            "yield kullanan bir generator (üretici) fonksiyon örneği:",
            "generator_ornek.py",
            "def sayac(baslangic, bitis):\n    sayi = baslangic\n    while sayi <= bitis:\n        yield sayi\n        sayi += 1\n\nfor s in sayac(1, 5):\n    print(s)"
        ),
        Template(
            listOf("decorator", "dekoratör"),
            "Bir fonksiyonun çalışma süresini ölçen basit bir decorator örneği:",
            "decorator_ornek.py",
            "import time\n\ndef zaman_olc(fonksiyon):\n    def sarmalayici(*args, **kwargs):\n        baslangic = time.time()\n        sonuc = fonksiyon(*args, **kwargs)\n        print(\"Süre:\", time.time() - baslangic)\n        return sonuc\n    return sarmalayici\n\n@zaman_olc\ndef bekle():\n    time.sleep(1)\n\nbekle()"
        ),
        Template(
            listOf("args kwargs", "değişken sayıda parametre"),
            "*args ve **kwargs kullanım örneği:",
            "args_kwargs.py",
            "def bilgi_yazdir(*args, **kwargs):\n    print(\"Args:\", args)\n    print(\"Kwargs:\", kwargs)\n\nbilgi_yazdir(1, 2, 3, isim=\"Ahmet\", yas=30)"
        ),
        Template(
            listOf("tarih", "datetime örneği", "bugünün tarihini python"),
            "datetime modülüyle güncel tarih ve saat alma:",
            "tarih_ornek.py",
            "from datetime import datetime\n\nsimdi = datetime.now()\nprint(simdi.strftime(\"%d-%m-%Y %H:%M:%S\"))"
        ),
        Template(
            listOf("regex", "düzenli ifade"),
            "re modülüyle basit bir düzenli ifade (regex) örneği - metinden e-posta bulma:",
            "regex_ornek.py",
            "import re\n\nmetin = \"İletişim: ornek@mail.com adresinden ulaşabilirsiniz.\"\neslesme = re.findall(r\"[\\w.-]+@[\\w.-]+\", metin)\nprint(eslesme)"
        ),
        Template(
            listOf("unittest", "test yaz", "birim test"),
            "unittest modülüyle basit bir birim test örneği:",
            "test_ornek.py",
            "import unittest\n\ndef topla(a, b):\n    return a + b\n\nclass TestToplama(unittest.TestCase):\n    def test_topla(self):\n        self.assertEqual(topla(2, 3), 5)\n\nif __name__ == \"__main__\":\n    unittest.main()"
        ),
        Template(
            listOf("threading", "iş parçacığı"),
            "threading modülüyle basit çoklu iş parçacığı örneği:",
            "threading_ornek.py",
            "import threading\n\ndef gorev(isim):\n    print(f\"{isim} çalışıyor\")\n\nt1 = threading.Thread(target=gorev, args=(\"Görev-1\",))\nt2 = threading.Thread(target=gorev, args=(\"Görev-2\",))\nt1.start()\nt2.start()\nt1.join()\nt2.join()"
        ),
        Template(
            listOf("argparse", "komut satırı argümanı"),
            "argparse ile komut satırından parametre alma örneği:",
            "argparse_ornek.py",
            "import argparse\n\nparser = argparse.ArgumentParser()\nparser.add_argument(\"--isim\", type=str, help=\"Adınızı girin\")\nargs = parser.parse_args()\nprint(f\"Merhaba, {args.isim}\")"
        ),
        Template(
            listOf("tkinter", "gui pencere", "arayüz penceresi"),
            "tkinter ile basit bir masaüstü penceresi (GUI) örneği:",
            "tkinter_ornek.py",
            "import tkinter as tk\n\npencere = tk.Tk()\npencere.title(\"ZekaTR Örnek Pencere\")\netiket = tk.Label(pencere, text=\"Merhaba, Dünya!\")\netiket.pack(padx=20, pady=20)\npencere.mainloop()"
        ),
        Template(
            listOf("sezar şifreleme", "caesar cipher"),
            "Klasik eğitim amaçlı Sezar Şifrelemesi (harfleri kaydırma) örneği:",
            "sezar_sifreleme.py",
            "def sezar_sifrele(metin, kaydirma):\n    sonuc = \"\"\n    for harf in metin:\n        if harf.isalpha():\n            taban = ord('A') if harf.isupper() else ord('a')\n            sonuc += chr((ord(harf) - taban + kaydirma) % 26 + taban)\n        else:\n            sonuc += harf\n    return sonuc\n\nsifreli = sezar_sifrele(\"Merhaba\", 3)\nprint(sifreli)"
        ),

        // ---------------- OYUN / EGLENCE ----------------
        Template(
            listOf("sayı tahmin oyunu", "tahmin oyunu"),
            "random modülüyle basit bir 'Sayı Tahmin Oyunu':",
            "sayi_tahmin.py",
            "import random\n\nhedef = random.randint(1, 100)\ntahmin = None\n\nwhile tahmin != hedef:\n    tahmin = int(input(\"1-100 arası bir sayı tahmin et: \"))\n    if tahmin < hedef:\n        print(\"Daha büyük bir sayı dene\")\n    elif tahmin > hedef:\n        print(\"Daha küçük bir sayı dene\")\n    else:\n        print(\"Tebrikler, bildin!\")"
        ),
        Template(
            listOf("xox", "tic tac toe"),
            "Konsolda basit bir XOX (Tic-Tac-Toe) tahtası gösterimi:",
            "xox.py",
            "tahta = [\" \"] * 9\n\ndef tahtayi_yazdir():\n    for i in range(0, 9, 3):\n        print(tahta[i], \"|\", tahta[i+1], \"|\", tahta[i+2])\n\ntahta[0], tahta[4], tahta[8] = \"X\", \"O\", \"X\"\ntahtayi_yazdir()"
        )
    )

    private fun normalize(text: String): String = text.lowercase(trLocale).trim()

    /**
     * Kullanicinin "kod yaz" istegine uyup uymadigini kontrol eder.
     * Uyuyorsa en yuksek skorlu CodeResult, uymuyorsa null doner.
     */
    fun tryGenerate(input: String): CodeResult? {
        val text = normalize(input)
        val kodTetikleyici = listOf(
            "kod yaz", "kod üret", "fonksiyon yaz", "program yaz", "script yaz",
            "python", "fayton", "algoritma yaz"
        )
        val genelKodIstegiVar = kodTetikleyici.any { text.contains(it) }

        var bestTemplate: Template? = null
        var bestScore = 0
        for (t in templates) {
            val score = t.triggers.count { text.contains(it) }
            if (score > bestScore) {
                bestScore = score
                bestTemplate = t
            }
        }

        if (bestTemplate != null && bestScore > 0) {
            return CodeResult(bestTemplate.fileName, bestTemplate.explanation, bestTemplate.code)
        }

        if (!genelKodIstegiVar) return null

        // Genel bir kod istegi var ama hangi konuda oldugu anlasilamadi
        return CodeResult(
            "ornek.py",
            "Tam olarak ne için kod istediğini anlayamadım. Şu an ${templates.size}+ farklı Python örneği biliyorum " +
                    "(sıralama, arama, sınıflar, dosya işlemleri, oyunlar, matematik ve daha fazlası). " +
                    "\"python'da bubble sort yaz\", \"python'da sınıf örneği\" gibi daha net bir istek yazarsan " +
                    "sana doğru örneği getirebilirim. Şimdilik basit bir örnek bırakıyorum:",
            "# Örnek: iki sayının toplamını yazdıran basit bir program\na = 3\nb = 4\nprint(\"Toplam:\", a + b)"
        )
    }
}
