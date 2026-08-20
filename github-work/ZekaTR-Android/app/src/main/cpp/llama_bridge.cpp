// ZekaTR - llama.cpp JNI koprusu.
// Kotlin tarafi: com.muhammed.zekatr.LocalLlmEngine
// Bu dosya llama.cpp submodule EKLENMEDEN derlenmez (bkz. README.md) -
// bu yuzden app/build.gradle icinde externalNativeBuild varsayilan olarak
// yorum satirinda birakildi.

#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include "llama.h"

#define TAG "ZekaTR-Llama"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

struct ZekaTrModelHandle {
    llama_model* model = nullptr;
    llama_context* ctx = nullptr;
    const llama_vocab* vocab = nullptr;
};

extern "C" JNIEXPORT jlong JNICALL
Java_com_muhammed_zekatr_LocalLlmEngine_nativeLoadModel(
        JNIEnv* env, jobject /*thiz*/, jstring modelPath, jint nCtx, jint nThreads) {
    const char* path = env->GetStringUTFChars(modelPath, nullptr);

    llama_backend_init();

    llama_model_params mparams = llama_model_default_params();
    mparams.n_gpu_layers = 0; // CPU-only: telefonlarda genel uyumluluk icin guvenli varsayilan

    llama_model* model = llama_model_load_from_file(path, mparams);
    env->ReleaseStringUTFChars(modelPath, path);

    if (!model) {
        LOGE("Model yuklenemedi");
        return 0;
    }

    llama_context_params cparams = llama_context_default_params();
    cparams.n_ctx = nCtx > 0 ? nCtx : 2048;
    cparams.n_threads = nThreads > 0 ? nThreads : 4;
    cparams.n_threads_batch = cparams.n_threads;

    llama_context* ctx = llama_init_from_model(model, cparams);
    if (!ctx) {
        LOGE("Context olusturulamadi");
        llama_model_free(model);
        return 0;
    }

    auto* handle = new ZekaTrModelHandle();
    handle->model = model;
    handle->ctx = ctx;
    handle->vocab = llama_model_get_vocab(model);
    return reinterpret_cast<jlong>(handle);
}

extern "C" JNIEXPORT void JNICALL
Java_com_muhammed_zekatr_LocalLlmEngine_nativeGenerate(
        JNIEnv* env, jobject thiz, jlong handlePtr, jstring prompt,
        jint maxTokens, jobject tokenCallback) {
    auto* handle = reinterpret_cast<ZekaTrModelHandle*>(handlePtr);
    if (!handle || !handle->ctx) return;

    const char* promptChars = env->GetStringUTFChars(prompt, nullptr);
    std::string promptStr(promptChars);
    env->ReleaseStringUTFChars(prompt, promptChars);

    jclass cbClass = env->GetObjectClass(tokenCallback);
    jmethodID onToken = env->GetMethodID(cbClass, "onToken", "(Ljava/lang/String;)V");

    // Prompt'u tokenize et
    const int nPromptMax = 4096;
    std::vector<llama_token> tokens(nPromptMax);
    int nTokens = llama_tokenize(handle->vocab, promptStr.c_str(), (int32_t) promptStr.size(),
                                  tokens.data(), nPromptMax, true, true);
    if (nTokens < 0) { LOGE("Tokenize hatasi"); return; }
    tokens.resize(nTokens);

    llama_batch batch = llama_batch_get_one(tokens.data(), (int32_t) tokens.size());
    if (llama_decode(handle->ctx, batch) != 0) { LOGE("Ilk decode hatasi"); return; }

    llama_sampler* sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(sampler, llama_sampler_init_temp(0.7f));
    llama_sampler_chain_add(sampler, llama_sampler_init_top_p(0.9f, 1));
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    for (int i = 0; i < maxTokens; i++) {
        llama_token newToken = llama_sampler_sample(sampler, handle->ctx, -1);
        if (llama_vocab_is_eog(handle->vocab, newToken)) break;

        char buf[256];
        int n = llama_token_to_piece(handle->vocab, newToken, buf, sizeof(buf), 0, true);
        if (n > 0) {
            jstring piece = env->NewStringUTF(std::string(buf, n).c_str());
            env->CallVoidMethod(tokenCallback, onToken, piece); // ChatAdapter'a akit (streaming)
            env->DeleteLocalRef(piece);
        }

        llama_batch nextBatch = llama_batch_get_one(&newToken, 1);
        if (llama_decode(handle->ctx, nextBatch) != 0) break;
    }

    llama_sampler_free(sampler);
}

extern "C" JNIEXPORT void JNICALL
Java_com_muhammed_zekatr_LocalLlmEngine_nativeFree(JNIEnv* /*env*/, jobject /*thiz*/, jlong handlePtr) {
    auto* handle = reinterpret_cast<ZekaTrModelHandle*>(handlePtr);
    if (!handle) return;
    if (handle->ctx) llama_free(handle->ctx);
    if (handle->model) llama_model_free(handle->model);
    delete handle;
}
