# Coling — Android Color Grading & Video Editor

Coling is a professional-grade mobile video editor and color grading app with a C++ rendering core, real-time node-based grades, and a multi-track editor.

## Key Features
*   **Media Page**: Import media, probe metadata using FFprobe.
*   **Edit Page**: Multi-track timeline, trim/split clips, title overlay.
*   **Color Page**: Canvas-drawn Node Graph, sweep-gradient Primaries wheels, custom RGB curve spline.
*   **Deliver Page**: Codec (H.264/H.265) & resolution presets, export rendering progress.
*   **Native JNI Bridge**: C++17 core for low-latency image processing using OpenGL ES 3.2.

## Project Structure
*   `/app`: Compose UI, navigation, Room DB integration.
*   `/native`: C++ JNI bridge and CMake build configurations.
*   `/docs`: System architecture & CI build steps.
*   `/.github/workflows`: GitHub Actions CI pipeline.
