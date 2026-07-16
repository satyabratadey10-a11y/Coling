#pragma once

#include <cstdint>
#include <cmath>
#include <algorithm>

namespace coling {

/// ASC-CDL style color wheel parameters (lift/gamma/gain/offset per channel)
struct ColorWheelParams {
    float liftR = 0.0f, liftG = 0.0f, liftB = 0.0f;
    float gammaR = 1.0f, gammaG = 1.0f, gammaB = 1.0f;
    float gainR = 1.0f, gainG = 1.0f, gainB = 1.0f;
    float offsetR = 0.0f, offsetG = 0.0f, offsetB = 0.0f;
    float contrast = 1.0f;
    float saturation = 1.0f;
};

/// Apply CDL-style lift/gamma/gain/offset to an RGBA8888 pixel buffer in-place.
/// Formula per channel: out = gain * (in + lift) ^ (1/gamma) + offset
/// Then contrast and saturation are applied as post-operations.
void applyPrimaries(uint8_t* pixels, int width, int height, int stride,
                    const ColorWheelParams& params);

/// Apply a 1D LUT (256 entries per channel) to an RGBA8888 buffer in-place.
/// lutR, lutG, lutB are each arrays of 256 uint8_t values.
void apply1DLUT(uint8_t* pixels, int width, int height, int stride,
                const uint8_t* lutR, const uint8_t* lutG, const uint8_t* lutB);

/// Apply a 3D LUT to an RGBA8888 buffer in-place.
/// lutData is a flattened NxNxN cube of RGB float triplets [0..1].
/// lutSize is N (typically 17, 33, or 65).
void apply3DLUT(uint8_t* pixels, int width, int height, int stride,
                const float* lutData, int lutSize);

/// Parse a .cube LUT file from memory buffer.
/// Returns allocated float array (caller owns) and sets outSize to the cube dimension.
/// Returns nullptr on parse failure.
float* parseCubeLUT(const char* cubeFileData, int dataLen, int& outSize);

/// Get color engine version string.
const char* getColorEngineVersion();

} // namespace coling
