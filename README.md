# FitTrack 🏃‍♂️📍

FitTrack is a modern, privacy-focused fitness tracking Android application. It utilizes Jetpack Compose for a slick UI, OSMDroid for offline-capable maps, and Android's foreground services for reliable background location tracking. 

## Features 🚀
*   📍 **Real-time GPS Tracking:** Track your runs, walks, and cycling sessions accurately.
*   🗺️ **Interactive Maps:** View your route seamlessly on interactive maps using OpenStreetMap (OSMDroid).
*   📊 **Workout Stats:** Real-time statistics including distance, duration, pace, and calories burned.
*   💾 **Local Storage:** All your data stays on your device using Room Database.
*   🎨 **Material 3 Design:** Beautiful, dynamic UI following modern Android design guidelines.

## Screenshots 📱
*(Placeholder for future screenshots of the Map, Dashboard, and History screens)*

---

## Getting the APK 📥

You can download and install the latest build of the app directly from this repository using GitHub Actions:

1.  **Fork** this repository to your own GitHub account.
2.  Go to the **Actions** tab in your repository.
3.  Click on the latest successful workflow run (named "Build & Sign APK").
4.  Scroll down to the **Artifacts** section and download the `FitTrack-APK` zip file.
5.  Extract the ZIP to find the `.apk` file.
6.  On your Android phone: Go to **Settings** → **Security** (or Apps) → **Install unknown apps** → enable the permission for your file manager.
7.  Tap the APK to install FitTrack.

---

## Battery Optimization Guide 🔋 ⚠️

To ensure FitTrack doesn't stop recording your location when you lock your screen, you **must** exclude the app from your phone's battery optimizations.

*   **Samsung:** Device Care → Battery → App Power Management → Never sleeping apps → add FitTrack
*   **Xiaomi / MIUI:** Security app → Manage apps → FitTrack → No restrictions
*   **Huawei / EMUI:** Phone Manager → App Launch → FitTrack → Manage manually → enable all
*   **OnePlus:** Settings → Battery → Battery Optimization → FitTrack → Don't optimize
*   **Stock Android (Pixel, Motorola):** Settings → Apps → FitTrack → Battery → Unrestricted

---

## Permissions Explained 🔒

| Permission | Why we need it |
| :--- | :--- |
| **Location (Foreground)** | Required to track your GPS position while the app is actively on screen. |
| **Background Location** | Necessary to continue tracking your workout even when you turn off the screen or switch to another app. |
| **Notifications** | Used by the foreground service to show an ongoing notification so Android knows the app is actively working and shouldn't be killed. |

---

## Optional: Signed Release Build 🔑

If you want the GitHub Action to produce a signed release APK rather than a debug APK, add the following Repository Secrets to your GitHub repository (Settings → Secrets and variables → Actions):

*   `KEYSTORE_BASE64`: The base64 encoded string of your `.jks` or `.keystore` file. (Command to generate: `base64 -i your_keystore.jks`)
*   `KEY_ALIAS`: Your key alias.
*   `KEY_PASSWORD`: The password for the key.
*   `STORE_PASSWORD`: The password for the keystore.

---

## Tech Stack 🛠️

*   **Language:** Kotlin
*   **UI Toolkit:** Jetpack Compose (Material 3)
*   **Architecture:** MVVM (Model-View-ViewModel) + Clean Architecture principles
*   **Dependency Injection:** Hilt
*   **Database:** Room
*   **Preferences:** Jetpack DataStore
*   **Maps:** OSMDroid
*   **Concurrency:** Kotlin Coroutines & Flows
*   **Location:** FusedLocationProviderClient (Google Play Services)

---

## Building Locally 💻

Requirements: Android Studio Hedgehog or newer.

1.  Clone the repository: `git clone https://github.com/yourusername/FitTrack.git`
2.  Open the project in Android Studio.
3.  Sync Gradle files.
4.  Connect an Android device or start an emulator.
5.  Click **Run** (Shift+F10) to build and install the debug version on your device.
