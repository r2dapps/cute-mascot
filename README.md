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

---

## 🎨 How Anyone Can Make Their Own Custom Character & Reactions with AI

You can create your own custom companion using **ChatGPT (GPT-4o / DALL-E 3)**, **Midjourney**, or any image generation tool! 

Both Windows Desktop and Android use standardized **3×3 grid sprite sheets**:
- **`directions.png`** (9 frames): The character turning their head/eyes to 8 directions around a center frame.
- **`reactions.png`** (9 frames): 9 emotional reactions triggered by taps, clicks, and idle peeking.

### 📐 3×3 Grid Layout Specifications

#### 1. Look Directions (`directions.png`)
```
+---------------+---------------+---------------+
|    Up-Left    |      Up       |   Up-Right    |
+---------------+---------------+---------------+
|     Left      | Center (Idle) |     Right     |
+---------------+---------------+---------------+
|   Down-Left   |     Down      |  Down-Right   |
+---------------+---------------+---------------+
```

#### 2. Emotional Reactions (`reactions.png`)
```
+---------------+---------------+---------------+
| Blink / Calm  |     Heart     |    Sparkle    |
+---------------+---------------+---------------+
|   Surprise    |  Wink / Smile |     Blush     |
+---------------+---------------+---------------+
| Sleepy (Zzz)  |     Dizzy     |   Delighted   |
+---------------+---------------+---------------+
```

---

### 📋 Ready-to-Paste AI Prompts (Copy & Paste into ChatGPT / Midjourney)

#### Prompt 1: 3×3 Look Directions Sheet (`directions.png`)
```text
A high-resolution 3x3 sprite sheet on a solid pure magenta (#FF00FF) background containing exactly 9 equal-sized square frames of the same cute chibi character.
The character must stay in the exact same spot in each frame, only turning their head and eyes to look in 9 directions matching a 3x3 grid:
Row 1: [Top-Left: looking up-left], [Top-Center: looking straight up], [Top-Right: looking up-right]
Row 2: [Mid-Left: looking left], [Mid-Center: looking directly forward at camera], [Mid-Right: looking right]
Row 3: [Bottom-Left: looking down-left], [Bottom-Center: looking straight down], [Bottom-Right: looking down-right]
Consistent character design across all 9 panels: cute chibi anime style with clean outlines, vibrant colors, same clothes and hair, neatly aligned on a strict 3x3 grid with no overlapping frames and no text or borders.
```

#### Prompt 2: 3×3 Reactions Sheet (`reactions.png`)
```text
A high-resolution 3x3 sprite sheet on a solid pure magenta (#FF00FF) background containing exactly 9 equal-sized square frames of the same cute chibi character expressing 9 distinct emotional reactions:
Row 1: [Top-Left: neutral blinking eyes], [Top-Center: floating pink love hearts with happy closed-eye smile], [Top-Right: sparkling excited eyes with golden glimmers]
Row 2: [Mid-Left: surprised gasp with wide round eyes (O_O)], [Mid-Center: playful cute wink with tongue slightly out], [Mid-Right: deep blushing cheeks, shy happy smile]
Row 3: [Bottom-Left: peaceful sleeping (Zzz float)], [Bottom-Center: dizzy cartoon spiral eyes (swirly)], [Bottom-Right: joyful radiant laugh / delighted wide grin]
Consistent character design matching the same character, clean outlines, no borders between cells, perfectly aligned 3x3 grid, zero text.
```

---

### 📲 Importing Custom Characters on Android

Our Android app allows you to import your custom character sheets directly from your phone!

1. Generate your character with AI and save the image to your phone gallery or files.
2. Open **Cute Mascot** on Android.
3. Scroll down to the **Character** section and tap **"⭐ Import Custom PNG"**.
4. Enter your character's name.
5. Select your 3×3 `directions.png` from your device storage.
6. Done! A new `⭐ <Name>` tile appears in your character selector, and your custom mascot immediately floats and animates on your screen!

---

### 💻 Adding Custom Characters on Windows Desktop

1. In the folder where `DesktopMascot.exe` is located, open `characters/`.
2. Create a new subfolder (e.g., `characters/my_hero/`).
3. Place your two 3×3 sheets inside:
   - `characters/my_hero/directions.png` *(required)*
   - `characters/my_hero/reactions.png` *(optional)*
4. Right-click the desktop mascot ➔ **🎭 Switch Character** ➔ Select your character!
*(Tip: To make backgrounds transparent automatically from solid color, run `python process_sheets.py`)*

---

## 📱 Android OS Compatibility & Sideload Guide

| Android Version | API Level | Status | Notes |
| :--- | :--- | :--- | :--- |
| **Android 14 & 15 & 16 Preview** | API 34+ | Supported | Requires 3-dots "Allow restricted settings" unlock for sideloaded APKs |
| **Android 13** | API 33 | Supported | Requires notification permission grant |
| **Android 12 & 12L** | API 31–32 | Supported | Exact alarm permission toggle available |
| **Android 8.0 – 11** | API 26–30 | Supported | Smooth overlay and touch tracking out of the box |

### 🔑 Android 13/14+ Sideloading Setup (3-Dots Unlock):
Because Android 13 and 14 protect sideloaded APKs by default, follow these quick steps:
1. When installing, tap **Install anyway** if prompted by Google Play Protect.
2. Open the **Cute Mascot** app.
3. In the setup guide card, tap **Step 1: Open App Info**.
4. In the top-right corner of the system App Info page, tap the **3 dots (⋮)** and select **Allow restricted settings** (enter phone PIN/fingerprint).
5. Return to Cute Mascot and tap **Step 2: Display Over Other Apps** ➔ toggle **ON**.
6. Tap **Step 3: Alarms & Reminders** ➔ toggle **ON** (allows exact health voice reminders).
7. Tap **Show mascot** ➔ Your companion floats happily on your screen!

---

## 🎭 Bundled page-mascot Cast

Many character sheets under `characters/` come from the open-source [page-mascot](https://github.com/nilbuild/page-mascot) project (see Credits). If you keep a local `reference-page-mascot/` clone, refresh the pack anytime with:

```bash
python sync_reference_characters.py
```

That copies every reference character folder into `characters/` (without overwriting personal characters).

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
├── characters/                # Hot-swappable character folders (includes page-mascot cast)
│   ├── mascot/                # Custom girl / fox mascot sheets
│   ├── chibi/ · guy/ · …      # App skins
│   └── fox/ · cat/ · …        # Bundled page-mascot characters (see Credits)
│
├── sync_reference_characters.py  # Refresh characters/ from local page-mascot clone
│
├── web/                       # Web Showcase & Playground
│   ├── index.html             # Interactive showcase
│   ├── style.css              # Modern aesthetic styling
│   └── mascot.js              # Mascot engine and sound synthesizer
│
└── reference-page-mascot/     # Optional local clone of page-mascot (gitignored)
```

---

## 🙏 Acknowledgements & Credits

Special thanks and appreciation to **[@nilbuild](https://github.com/nilbuild)** for creating the wonderful **[page-mascot](https://github.com/nilbuild/page-mascot)** open-source project.

The 3×3 cursor-tracking architecture, angle sector math, and boop physics in this repository are deeply inspired by and built upon the foundational work established in `page-mascot`.

**Character sheets:** many bundled folders under `characters/` (fox, cat, otter, knight, and dozens more) are the original page-mascot sprite packs, included here so DesktopMascot can ship that cast without requiring a separate clone. Please keep attribution to [@nilbuild / page-mascot](https://github.com/nilbuild/page-mascot) when redistributing those assets.
---

## 📄 License

MIT License. Feel free to use and customize for your own desktop pets and websites!
