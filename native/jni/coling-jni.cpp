#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "ColingNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_coling_NativeBridge_getNativeVersion(JNIEnv* env, jobject /* this */) {
    std::string version = "1.0.0-Core (C++17, GLES 3.2, FFmpeg-Kit-Next)";
    return env->NewStringUTF(version.c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_coling_NativeBridge_initNativeContext(JNIEnv* env, jobject /* this */, jint width, jint height, jboolean useVulkan) {
    LOGI("Initializing native rendering context: %dx%d (Vulkan=%s)", width, height, useVulkan ? "true" : "false");
    // Context initialization logic (OpenGL ES 3.2 EGL / Vulkan setup)
    return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_coling_NativeBridge_processFrame(JNIEnv* env, jobject /* this */, jint inputTexId, jint outputTexId, jstring nodeGraphJson) {
    const char* jsonStr = env->GetStringUTFChars(nodeGraphJson, nullptr);
    if (jsonStr == nullptr) {
        return JNI_FALSE;
    }
    LOGI("Processing frame. Input Tex: %d, Output Tex: %d, Nodes: %s", inputTexId, outputTexId, jsonStr);
    
    // Release Java string resources
    env->ReleaseStringUTFChars(nodeGraphJson, jsonStr);
    return JNI_TRUE;
}

#include <android/bitmap.h>
#include "color_engine.h"

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_coling_NativeBridge_processBitmap(JNIEnv* env, jobject /* this */, jobject bitmap,
                                                   jfloat liftR, jfloat liftG, jfloat liftB,
                                                   jfloat gammaR, jfloat gammaG, jfloat gammaB,
                                                   jfloat gainR, jfloat gainG, jfloat gainB,
                                                   jfloat contrast, jfloat saturation) {
    AndroidBitmapInfo info;
    void* pixels = nullptr;
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) {
        LOGE("Failed to get bitmap info");
        return JNI_FALSE;
    }
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format is not RGBA_8888");
        return JNI_FALSE;
    }
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) {
        LOGE("Failed to lock bitmap pixels");
        return JNI_FALSE;
    }

    coling::ColorWheelParams params;
    params.liftR = liftR;
    params.liftG = liftG;
    params.liftB = liftB;
    params.gammaR = gammaR;
    params.gammaG = gammaG;
    params.gammaB = gammaB;
    params.gainR = gainR;
    params.gainG = gainG;
    params.gainB = gainB;
    params.contrast = contrast;
    params.saturation = saturation;

    coling::applyPrimaries(reinterpret_cast<uint8_t*>(pixels), info.width, info.height, info.stride, params);

    AndroidBitmap_unlockPixels(env, bitmap);
    return JNI_TRUE;
}
