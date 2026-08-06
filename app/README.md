# PolyStudio 3D 🎨📦

**PolyStudio 3D** is an advanced 3D design and animation studio application built for Android. It features 3D model visualization, GLB & FBX import/export, 3D surface painting, skeletal animation controls, dynamic geometric primitive creation, and polygon mesh reduction tools.

---

## 📱 Pre-compiled APK Included

The pre-compiled, ready-to-install Android package file is included directly in this repository root:

- **File Name:** `polystudio3d.apk`
- **Location:** `./polystudio3d.apk`

### Supported Architectures
The APK is compiled with native binary support for:
- `arm64-v8a` (64-bit ARM devices)
- `armeabi-v7a` (32-bit ARM devices)
- `x86` / `x86_64` (Emulators & Intel-based Android devices)

---

## ✨ Features

- **3D Model Inspection & Rendering**: View, rotate, scale, and manipulate 3D models with high performance.
- **Lighting & Materials**: Customize ambient, directional, and point lights alongside PBR materials.
- **GLB & FBX Support**: Import and export standard 3D asset formats.
- **Mesh & Polygon Tools**: Polygon mesh reduction and geometric primitive creation.
- **Animation & Rigging**: Preview and adjust skeletal animations and bone hierarchies.

---

## 🚀 How to Install

1. Download `polystudio3d.apk` directly from this repository or from the **Releases** section on GitHub.
2. Transfer the `.apk` file to your Android device.
3. Open the file on your device and follow the prompt to install (allow "Install from Unknown Sources" if prompted).

---

## 🛠️ Building from Source

### Prerequisites
- Android Studio Ladybug or newer
- JDK 17+
- Android SDK (API Level 34+)

### Build Commands
To build the debug APK locally:

```bash
./gradlew assembleDebug
```

The output APK will be placed at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 📌 Publishing to GitHub Releases

If you want to create a GitHub Release with this APK:

1. Push this repository to GitHub via the **Push to GitHub** option in AI Studio.
2. On your GitHub repository page, navigate to **Releases** on the right sidebar and click **Draft a new release**.
3. Create a tag (e.g. `v1.0.0`), enter a title (e.g. `PolyStudio 3D v1.0.0 Release`), and upload or link `polystudio3d.apk`.
4. Click **Publish release**.
