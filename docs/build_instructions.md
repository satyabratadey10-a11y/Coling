# Build and Compilation Instructions

Since Coling uses native NDK C++ dependencies and target device architectures are highly constrained (4KB page sizes on Snapdragon 662), compilation is orchestrated on GitHub Actions.

---

## 1. Remote Build via GitHub Actions CI/CD (Recommended)

1. Commit and push the project to a GitHub repository:
   ```bash
   git init
   git add .
   git commit -m "Scaffold project"
   git remote add origin <your-github-repo-url>
   git push -u origin main
   ```
2. The GitHub Action workflow (`.github/workflows/ci.yml`) will automatically trigger:
   * Installs SDK and NDK components.
   * Downloads and cross-compiles FFmpegKitNext for `arm64-v8a`.
   * Builds the application and produces Debug + Release APKs.
3. Retrieve compiled APKs from the **Actions** tab on your GitHub repository page under "Artifacts".

---

## 2. Local Gradle Settings for Proot/Termux (No Compilation Rule)

*   Avoid running `./gradlew assembleDebug` directly on Termux/proot to prevent resource exhaustion on Snapdragon 662.
*   Only use local Gradle for code validation and dependency synchronization checks if desired.
