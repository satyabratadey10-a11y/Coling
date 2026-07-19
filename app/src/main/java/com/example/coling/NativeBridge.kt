package com.example.coling

import android.util.Log

object NativeBridge {
    init {
        try {
            System.loadLibrary("coling_native")
            Log.d("NativeBridge", "Successfully loaded coling_native library")
        } catch (e: UnsatisfiedLinkError) {
            Log.e("NativeBridge", "Failed to load coling_native library", e)
        }
    }

    /**
     * Get version string from native C++ core.
     */
    external fun getNativeVersion(): String

    /**
     * Initialize the OpenGL ES or Vulkan processing context.
     * @param width Width of the render target/preview.
     * @param height Height of the render target/preview.
     * @param useVulkan Feature flag to enable Vulkan compute instead of GLES.
     */
    external fun initNativeContext(width: Int, height: Int, useVulkan: Boolean): Boolean

    /**
     * Process a video frame from input buffer/texture to output buffer/texture.
     * Passes a serialized JSON string containing node graph parameters.
     */
    external fun processFrame(inputTexId: Int, outputTexId: Int, nodeGraphJson: String): Boolean

    /**
     * Process a Bitmap in-place with Lift, Gamma, Gain, Contrast, and Saturation settings.
     */
    external fun processBitmap(
        bitmap: Any,
        liftR: Float, liftG: Float, liftB: Float,
        gammaR: Float, gammaG: Float, gammaB: Float,
        gainR: Float, gainG: Float, gainB: Float,
        contrast: Float, saturation: Float
    ): Boolean
}
