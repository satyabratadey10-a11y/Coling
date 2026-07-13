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
