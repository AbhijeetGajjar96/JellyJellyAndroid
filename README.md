# JellyJelly1

JellyJelly1 is an Android app built with Jetpack Compose, featuring a custom camera screen and a modern UI. The app supports single and dual camera modes, video recording with duration and quality selection, and sound toggling. It is designed to work reliably across a wide range of Android devices and emulators.

## Features
- **Camera UI:**
  - Single and dual camera preview (if supported by device)
  - Toggle between front and back cameras
  - Video recording with animated record button
  - Select video duration (15s/60s)
  - Choose video quality (Low/Medium/High)
  - Toggle sound recording on/off
  - Robust error handling for camera operations
- **Permissions:**
  - Requests CAMERA and RECORD_AUDIO permissions
  - Handles permission flow gracefully
- **Device Compatibility:**
  - Dynamically detects available cameras
  - Disables dual camera on unsupported devices
  - Prevents crashes on devices/emulators with missing camera features

## Project Structure
- `app/src/main/java/com/example/jellyjelly1/`
  - `ui/camera/CameraScreen.kt` – Main camera UI and logic
  - `ui/feed/` – Feed and video player screens
  - `network/` – API and Retrofit client
  - `repository/` – Data and camera repository
  - `model/` – Data models for video and feed
  - `MainActivity.kt` – App entry point

## Setup & Build
1. **Clone the repository:**
   ```sh
   git clone <repo-url>
   cd jellyJelly1
   ```
2. **Open in Android Studio.**
3. **Build and run on a device or emulator.**

## Notes
- Dual camera preview is only available on devices that support logical multi-camera (rare; most emulators and many phones do not support this).
- Video files are named to reflect quality and sound status.
- The app follows best practices for Camera2 API and MediaRecorder integration.

## License
For educational purposes only.
