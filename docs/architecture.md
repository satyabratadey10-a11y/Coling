# Coling — Architectural Specification

Coling is a professional-grade mobile video editor and color grading application designed to operate efficiently on mid-to-low-tier Android devices (specifically targeting Snapdragon 662 and Adreno 610 architecture).

---

## 1. System Topology

```
+--------------------------------------------------------+
|                      Kotlin Layer                      |
|  - Jetpack Compose UI (Screens, Nodes, Color Wheels)   |
|  - Navigation, States, Room DB (Project Persistence)  |
+--------------------------+-----------------------------+
                           |
                           v JNI Bridge
+--------------------------------------------------------+
|                      Native Layer                      |
|  - JNI Entrypoint (coling-jni.cpp)                     |
|  - OpenGL ES 3.2 Rendering Context                     |
|  - Shader-based primaries correction (Lift/Gamma/Gain)  |
|  - FFmpeg Decode/Encode Engine (FFmpeg-Kit-Next)       |
+--------------------------------------------------------+
```

---

## 2. Platform Constraints & Targets

*   **Memory Footprint**: Frames are processed sequentially via hardware-accelerated texture mapping or downscaled CPU proxies. Full-resolution frames are never cached globally in RAM.
*   **Memory Page Size**: Targets the **4KB page size** model of older/standard Android releases (Android 13, ARMv8.0-A). All prebuilt `.so` and `.aar` dependencies are compiled with 4KB alignment.
*   **Acceleration APIs**:
    *   *Primary*: OpenGL ES 3.2 compute/fragment shaders.
    *   *Secondary (Feature Flag)*: Vulkan 1.1 compute context.
    *   *Fallback*: Pure CPU parallel pixel loops.
    *   No desktop dependencies (CUDA/OpenCL).

---

## 3. Library Integration & Workarounds

| Subsystem | Strategy | Reason |
|---|---|---|
| **FFmpeg** | FFmpegKitNext (source compilation) | Compiled via GitHub Actions and cached to avoid runtime distribution lifecycle dropouts. |
| **OCIO / Color** | Trimmed custom CPU 3D LUT evaluator | Avoids bringing in heavy desktop OpenColorIO/Imath dependencies. |
| **OpenImageIO** | Swapped with `stb_image` / `stb_image_write` | Light single-header alternatives for raw file handling. |

---

## 4. Database Schema (Room)

*   `projects`: Holds unique project profiles.
*   `timeline_clips`: Tracks clip alignments, video/audio tracks, and durations.
*   `color_nodes`: Serial/Parallel nodes representing color wheels, curve points, and HSL masks.
