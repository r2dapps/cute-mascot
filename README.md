<p align="center">
  <img src="assets/icon.png" width="160" height="160" alt="Cute Desktop Mascot Icon" style="border-radius: 50%; box-shadow: 0 8px 32px rgba(255, 105, 180, 0.3);">
</p>

# 🎀 Cute Desktop & Mobile Mascot (v1.3.0)

An interactive, transparent mascot for Windows Desktop and Android Mobile, featuring customizable characters (cute anime girl with spectacles & kurti, companion guy, fox chibi), talking reminders in Godavari Telugu and Japanese anime voices with phoneme lip-sync, hot-swappable character sheets, and playful idle animations.

[![Release](https://img.shields.io/badge/release-v1.3.0-pink.svg)](https://github.com/r2dapps/cute-mascot/releases)
[![Live Web Demo](https://img.shields.io/badge/Live%20Web%20Demo-Interactive%20Showcase-ff69b4.svg)](https://r2dapps.github.io/cute-mascot/web/)
[![Android](https://img.shields.io/badge/Android-APK%20Ready-green.svg)](https://github.com/r2dapps/cute-mascot/releases/latest/download/CuteMascot.apk)
[![Windows](https://img.shields.io/badge/Windows-EXE%20Ready-blue.svg)](https://github.com/r2dapps/cute-mascot/releases/latest/download/DesktopMascot.exe)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

---

## 📦 Downloads & Live Demo (v1.3.0)

| Platform | Link / Download | Features |
| :--- | :--- | :--- |
| 🌐 **Live Web Demo** | [**Open Web Companion**](https://r2dapps.github.io/cute-mascot/web/) | Zero install, interactive browser mascot, mouse tracking, squish boop sounds, live character switching |
| 📱 **Android Mobile** | [**CuteMascot.apk**](https://github.com/r2dapps/cute-mascot/releases/download/v1.3.0/CuteMascot.apk) *(20.4 MB)* | Floating overlay, real-time size slider, SoundPool boops, full-screen touch tracking, Gyro tilt, health reminders |
| 💻 **Windows Desktop** | [**DesktopMascot.exe**](https://github.com/r2dapps/cute-mascot/releases/download/v1.3.0/DesktopMascot.exe) *(63.5 MB)* | Standalone Win32 per-pixel transparency, global mouse tracking, Telugu/Japanese voice reminders, lip-sync, tray controls |

---

## ✨ Features

- **🖥️ True Desktop Floating Window**: Native Win32 `UpdateLayeredWindow` with per-pixel alpha transparency. Frameless, zero background box, always floating on your desktop or over active windows.
- **📱 Android Floating Overlay**: Floats on top of all phone apps using clean overlay permissions. Full-screen in-app finger tracking and gyroscope motion tilt tracking.
- **🗣️ Talking Reminders & Voice Notes**:
  - Periodically reminds you to drink water, stretch, correct posture, and smile.
  - Dual voice modes: **Godavari Telugu** (sweet, caring) and **Japanese Anime** (cute, soft).
  - Synchronized real-time phoneme lip-sync (`A`, `E`, `I`, `O`, `U`, `M`).
- **👀 Global Cursor & Touch Tracking**: Head smoothly turns and follows your cursor or touch anywhere across your screens.
- **💖 Interactive Boops & Audio Engine**:
  - Tap or click to boop! Rubbery squish bounce physics with cheerful sound effects.
  - Multi-layer audio playback with embedded dialogue fallbacks.
- **🌸 Idle Peeking & Playful Behaviors**:
  - Natural blinks, playful peeks, cute winks, and idle animations.
- **🎭 Multi-Character Roster**:
  - **Mascot**: Classic Fox Chibi companion.
  - **Chibi Girl**: High-detail anime girl with spectacles, wavy dark hair, pearl hairpin, and pink kurti.
  - **Companion Guy**: Stylized partner with curly hair and glasses, complete with full 9-reaction sprite sheet.
  - Hot-swappable on both desktop and mobile!
- **⚙️ Native Controls & Customization**:
  - Desktop: Tray context menu with size presets, sound toggle, teleport, reminder trigger, startup launch.
  - Android: Modern settings dashboard with real-time size slider (100–260 dp), preset chips, and sound effects test.

---

## 🚀 Quick Start

### 1. Launch Directly on Windows Desktop (Standalone EXE)
Simply double-click the compiled application:
```
DesktopMascot.exe
```
*(Zero setup required — self-contained executable with custom icon and no terminal prompt)*

Or run directly via Python:
```bash
python app.py
```

### 2. Launch Interactive Web Showcase
Open `web/index.html` directly in your browser or run:
```bash
python -m http.server 8000
```
and visit `http://localhost:8000/web/`.

---

## 🌐 Setting Up GitHub Pages

This repository is pre-configured with a GitHub Actions workflow for zero-config GitHub Pages deployment:

1. Push the code to your repository:
   ```bash
   git push origin main --tags
   ```
2. On GitHub, go to your repository:
   **`https://github.com/r2dapps/cute-mascot`**
3. Navigate to **Settings** ➔ **Pages** (under Code and automation in the left sidebar).
4. Under **Build and deployment**:
   - **Source**: Select **GitHub Actions** (it will automatically use `.github/workflows/deploy-pages.yml`).
5. Your live interactive mascot showcase will be published at:
   **`https://r2dapps.github.io/cute-mascot/web/`**

---

## 🎭 How to Swap Different Character Sheets

You can easily use different character sheets with `DesktopMascot.exe` or `app.py`!

### Method 1: Via the Right-Click Menu
1. Right-click the mascot on your desktop.
2. Hover over **🎭 Switch Character**.
3. Select any installed character, or click **📂 Open Characters Folder...** to open the folder in Windows File Explorer.

### Method 2: Adding Your Own Custom Characters
1. In the folder where `DesktopMascot.exe` is located, go into the `characters/` directory:
   ```
   characters/
   ├── mascot/
   │   ├── directions.png
   │   └── reactions.png
   ├── my_hero/
   │   ├── directions.png
   │   └── reactions.png
   └── cat/
       ├── directions.png
       └── reactions.png
   ```
2. Create a new folder (e.g. `characters/my_hero/`).
3. Add your two 3×3 grid sprite sheets:
   - **`directions.png`** *(required)*: 3×3 grid with 8 head directions + center looking forward.
   - **`reactions.png`** *(optional)*: 3×3 grid with 9 emotion tiles (hearts, sparkles, dizzy, etc.). If omitted, neutral center is used.
4. Right-click the mascot -> **🎭 Switch Character** -> Your character appears automatically!
5. Your active character is remembered across app restarts in `mascot_config.json`.

---

## 🚀 Run on Windows Startup

To make the Mascot start automatically whenever you log into your PC:
1. Right-click the mascot.
2. Click **🚀 Run on Startup** so a checkmark `✓` appears.
3. Done! It adds an entry in `HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Run` without requiring administrator privileges.
4. To disable it, simply click **🚀 Run on Startup** again to uncheck it.

---

## 🏷️ Versioning & Releases Workflow

We follow standard Semantic Versioning (`vMAJOR.MINOR.PATCH`) with Git tags for clean release management and easy rollbacks:

### To create and publish a new release:
```bash
# 1. Stage and commit your changes
git add .
git commit -m "feat: description of release"

# 2. Create an annotated git tag
git tag -a v1.0.0 -m "Release v1.0.0: Cute Desktop Mascot"

# 3. Push commits and tags to GitHub
git push origin main --tags
```

### To rollback to a previous version if needed:
```bash
# View all release tags
git tag

# Checkout specific tag into a clean branch
git checkout -b rollback-branch v1.0.0
```

On GitHub, you can also navigate to **Releases** ➔ **Draft a new release**, select tag `v1.0.0`, attach `DesktopMascot.exe` as a pre-compiled binary download for users, and publish!

---

## 📁 Project Structure

```
Desktop-Mascot/
├── DesktopMascot.exe          # Standalone Windows executable
├── app.py                     # Native Win32 Desktop Mascot application
├── index.html                 # Root redirect for GitHub Pages
├── process_sheets.py          # Sprite sheets chroma-key & alpha pipeline
├── mascot_source.png          # Original character reference image
├── mascot_config.json         # User settings (character, size, sound, startup)
│
├── .github/workflows/
│   └── deploy-pages.yml       # GitHub Actions workflow for GitHub Pages
│
├── assets/                    # Bundled default assets
│   ├── mascot-directions.png  # 3x3 Aligned Head Directions Atlas (PNG)
│   ├── mascot-directions.webp # 3x3 Aligned Head Directions Atlas (WebP)
│   ├── mascot-reactions.png   # 3x3 Aligned Facial Expressions Atlas (PNG)
│   ├── mascot-reactions.webp  # 3x3 Aligned Facial Expressions Atlas (WebP)
│   ├── icon.png               # Application icon (PNG)
│   └── icon.ico               # Application tray icon (ICO)
│
├── characters/                # Hot-swappable character folders
│   └── mascot/                # Custom Girl Mascot character sheets
│
├── web/                       # Web Showcase & Playground
│   ├── index.html             # Interactive showcase
│   ├── style.css              # Modern aesthetic styling
│   └── mascot.js              # Mascot engine and sound synthesizer
│
└── reference-page-mascot/     # Local reference repository (ignored by git)
```

---

## 🙏 Acknowledgements & Credits

Special thanks and appreciation to **[@nilbuild](https://github.com/nilbuild)** for creating the wonderful **[page-mascot](https://github.com/nilbuild/page-mascot)** open-source project. 

The 3×3 cursor-tracking architecture, angle sector math, and boop physics in this repository are deeply inspired by and built upon the foundational work established in `page-mascot`.

---

## 📄 License

MIT License. Feel free to use and customize for your own desktop pets and websites!
