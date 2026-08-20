# ZekaTR Yerel GGUF Cikarim Motoru (llama.cpp entegrasyonu)

Bu klasordeki `CMakeLists.txt` ve `llama_bridge.cpp` GERCEK, calismaya hazir
JNI koprusudur — ama llama.cpp'nin kendi kaynak kodunu (C/C++, ~binlerce
dosya) bu depoya biz gommedik: hem depo boyutu hem lisans/guncelleme
nedenleriyle bunun bir git submodule olarak eklenmesi doğru olan yontemdir
(bircok gercek Android LLM uygulamasi - orn. resmi llama.cpp'nin kendi
`examples/llama.android` ornegi - ayni yapiyi kullanir).

## Kurulum (bilgisayarinda, apk'yi derlemeden once)

```bash
cd ZekaTR-Android-2.0.0-build.11
git submodule add https://github.com/ggerganov/llama.cpp app/src/main/cpp/llama.cpp
git submodule update --init --recursive
```

Sonra `app/build.gradle` icindeki iki yorum blogunu ac (arama: "YEREL GGUF
CIKARIM MOTORU"). Android Studio "Sync" dedikten sonra NDK'yi otomatik
indirir (SDK Manager'da NDK (Side by side) ve CMake kurulu olmali).

## Bu ne yapiyor?

- `CMakeLists.txt`: llama.cpp'yi (submodule) alt proje olarak derler, bizim
  `llama_bridge.cpp` dosyamizi da derleyip `libzekatr_llama.so` uretir.
- `llama_bridge.cpp`: Kotlin tarafindaki `LocalLlmEngine.kt` icinden
  cagrilan `nativeLoadModel`, `nativeGenerate`, `nativeFree` JNI
  fonksiyonlarini llama.cpp'nin `llama_model_load_from_file`,
  `llama_decode`, sampler API'lerine baglar. Token uretimi TEK TEK,
  bir Kotlin callback'ine (`onToken`) akitilir (streaming) - bu sayede
  sohbet ekraninda kelime kelime yazma efekti gercek model ciktisindan
  gelir (sahte "daktilo" animasyonu degil).
- Kotlin tarafi zaten `LocalLlmEngine.kt` icinde hazir; submodule
  eklenmeden once de proje sorunsuz derlenir (native cagrilar UnsatisfiedLinkError
  yakalanip kullaniciya acik bir mesaj gosterir; app CRASH OLMAZ).

## Model secimi ile iliskisi

`ModelPaths.kt` + `ggufmodel/` klasoru (bkz. app/src/main/assets/ggufmodel/README.txt)
zaten hangi .gguf dosyasinin aktif oldugunu belirliyor; `LocalLlmEngine.loadModel(path)`
bu dosya yolunu native tarafa geciriyor. Yani submodule'u ekleyip derledigin an,
"ZekaTR Thinking Model" (senin qwen2.5-0.5b-instruct-q2_k.gguf dosyan) VE
kullanicinin kendi ekledigi herhangi bir .gguf (Qwen/Gemma/Llama/Mistral/Phi -
llama.cpp GGUF formatini destekleyen HER model) calisir hale gelir.
