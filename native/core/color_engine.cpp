#include "color_engine.h"
#include <cstring>
#include <cstdlib>
#include <cstdio>

namespace coling {

static inline float clamp01(float v) {
    return std::max(0.0f, std::min(1.0f, v));
}

static inline uint8_t toU8(float v) {
    return static_cast<uint8_t>(clamp01(v) * 255.0f + 0.5f);
}

void applyPrimaries(uint8_t* pixels, int width, int height, int stride,
                    const ColorWheelParams& params) {
    if (!pixels || width <= 0 || height <= 0) return;

    const float invGammaR = (params.gammaR > 0.001f) ? (1.0f / params.gammaR) : 1.0f;
    const float invGammaG = (params.gammaG > 0.001f) ? (1.0f / params.gammaG) : 1.0f;
    const float invGammaB = (params.gammaB > 0.001f) ? (1.0f / params.gammaB) : 1.0f;

    for (int y = 0; y < height; ++y) {
        uint8_t* row = pixels + y * stride;
        for (int x = 0; x < width; ++x) {
            uint8_t* px = row + x * 4; // RGBA
            float r = px[0] / 255.0f;
            float g = px[1] / 255.0f;
            float b = px[2] / 255.0f;
            // Alpha untouched

            // CDL: out = gain * (in + lift) ^ (1/gamma) + offset
            r = params.gainR * std::pow(std::max(0.0f, r + params.liftR), invGammaR) + params.offsetR;
            g = params.gainG * std::pow(std::max(0.0f, g + params.liftG), invGammaG) + params.offsetG;
            b = params.gainB * std::pow(std::max(0.0f, b + params.liftB), invGammaB) + params.offsetB;

            // Contrast (pivot at 0.5)
            if (params.contrast != 1.0f) {
                r = (r - 0.5f) * params.contrast + 0.5f;
                g = (g - 0.5f) * params.contrast + 0.5f;
                b = (b - 0.5f) * params.contrast + 0.5f;
            }

            // Saturation (Rec.709 luminance weighting)
            if (params.saturation != 1.0f) {
                float luma = 0.2126f * r + 0.7152f * g + 0.0722f * b;
                r = luma + params.saturation * (r - luma);
                g = luma + params.saturation * (g - luma);
                b = luma + params.saturation * (b - luma);
            }

            px[0] = toU8(r);
            px[1] = toU8(g);
            px[2] = toU8(b);
        }
    }
}

void apply1DLUT(uint8_t* pixels, int width, int height, int stride,
                const uint8_t* lutR, const uint8_t* lutG, const uint8_t* lutB) {
    if (!pixels || !lutR || !lutG || !lutB || width <= 0 || height <= 0) return;

    for (int y = 0; y < height; ++y) {
        uint8_t* row = pixels + y * stride;
        for (int x = 0; x < width; ++x) {
            uint8_t* px = row + x * 4;
            px[0] = lutR[px[0]];
            px[1] = lutG[px[1]];
            px[2] = lutB[px[2]];
        }
    }
}

void apply3DLUT(uint8_t* pixels, int width, int height, int stride,
                const float* lutData, int lutSize) {
    if (!pixels || !lutData || lutSize < 2 || width <= 0 || height <= 0) return;

    const int N = lutSize;
    const float scale = static_cast<float>(N - 1);

    for (int y = 0; y < height; ++y) {
        uint8_t* row = pixels + y * stride;
        for (int x = 0; x < width; ++x) {
            uint8_t* px = row + x * 4;
            float r = px[0] / 255.0f;
            float g = px[1] / 255.0f;
            float b = px[2] / 255.0f;

            // Map to LUT coordinates
            float fr = clamp01(r) * scale;
            float fg = clamp01(g) * scale;
            float fb = clamp01(b) * scale;

            int r0 = std::min((int)fr, N - 2);
            int g0 = std::min((int)fg, N - 2);
            int b0 = std::min((int)fb, N - 2);

            float dr = fr - r0;
            float dg = fg - g0;
            float db = fb - b0;

            // Trilinear interpolation
            auto lutAt = [&](int ri, int gi, int bi, int ch) -> float {
                return lutData[((bi * N + gi) * N + ri) * 3 + ch];
            };

            float outR = 0, outG = 0, outB = 0;
            for (int ch = 0; ch < 3; ++ch) {
                float c000 = lutAt(r0,     g0,     b0,     ch);
                float c100 = lutAt(r0 + 1, g0,     b0,     ch);
                float c010 = lutAt(r0,     g0 + 1, b0,     ch);
                float c110 = lutAt(r0 + 1, g0 + 1, b0,     ch);
                float c001 = lutAt(r0,     g0,     b0 + 1, ch);
                float c101 = lutAt(r0 + 1, g0,     b0 + 1, ch);
                float c011 = lutAt(r0,     g0 + 1, b0 + 1, ch);
                float c111 = lutAt(r0 + 1, g0 + 1, b0 + 1, ch);

                float c00 = c000 + dr * (c100 - c000);
                float c10 = c010 + dr * (c110 - c010);
                float c01 = c001 + dr * (c101 - c001);
                float c11 = c011 + dr * (c111 - c011);

                float c0 = c00 + dg * (c10 - c00);
                float c1 = c01 + dg * (c11 - c01);

                float val = c0 + db * (c1 - c0);
                if (ch == 0) outR = val;
                else if (ch == 1) outG = val;
                else outB = val;
            }

            px[0] = toU8(outR);
            px[1] = toU8(outG);
            px[2] = toU8(outB);
        }
    }
}

float* parseCubeLUT(const char* cubeFileData, int dataLen, int& outSize) {
    if (!cubeFileData || dataLen <= 0) return nullptr;

    outSize = 0;
    int lutSize = 0;
    float* lutData = nullptr;
    int dataIndex = 0;

    // Simple line-by-line parser
    const char* ptr = cubeFileData;
    const char* end = cubeFileData + dataLen;

    while (ptr < end) {
        // Skip whitespace
        while (ptr < end && (*ptr == ' ' || *ptr == '\t')) ++ptr;

        if (ptr >= end) break;

        // Skip comments and empty lines
        if (*ptr == '#' || *ptr == '\n' || *ptr == '\r') {
            while (ptr < end && *ptr != '\n') ++ptr;
            if (ptr < end) ++ptr;
            continue;
        }

        // Check for LUT_3D_SIZE
        if (strncmp(ptr, "LUT_3D_SIZE", 11) == 0) {
            ptr += 11;
            while (ptr < end && (*ptr == ' ' || *ptr == '\t')) ++ptr;
            lutSize = atoi(ptr);
            if (lutSize >= 2 && lutSize <= 256) {
                lutData = new float[lutSize * lutSize * lutSize * 3];
            }
            while (ptr < end && *ptr != '\n') ++ptr;
            if (ptr < end) ++ptr;
            continue;
        }

        // Skip TITLE, DOMAIN_MIN, DOMAIN_MAX, LUT_1D_SIZE, other keywords
        if ((*ptr >= 'A' && *ptr <= 'Z') || (*ptr >= 'a' && *ptr <= 'z')) {
            while (ptr < end && *ptr != '\n') ++ptr;
            if (ptr < end) ++ptr;
            continue;
        }

        // Parse data line: three floats
        if (lutData && dataIndex < lutSize * lutSize * lutSize * 3) {
            float r, g, b;
            char line[256];
            int lineLen = 0;
            const char* lineStart = ptr;
            while (ptr < end && *ptr != '\n' && *ptr != '\r' && lineLen < 255) {
                line[lineLen++] = *ptr++;
            }
            line[lineLen] = '\0';

            if (sscanf(line, "%f %f %f", &r, &g, &b) == 3) {
                lutData[dataIndex++] = r;
                lutData[dataIndex++] = g;
                lutData[dataIndex++] = b;
            }
        }

        while (ptr < end && *ptr != '\n') ++ptr;
        if (ptr < end) ++ptr;
    }

    if (lutData && dataIndex == lutSize * lutSize * lutSize * 3) {
        outSize = lutSize;
        return lutData;
    }

    // Parse failure or incomplete data
    delete[] lutData;
    outSize = 0;
    return nullptr;
}

const char* getColorEngineVersion() {
    return "1.0.0-CDL";
}

} // namespace coling
