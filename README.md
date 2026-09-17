<p align="center">
  <img src="assets/icon.png" width="160" height="160" alt="Cute Desktop Mascot Icon" style="border-radius: 50%; box-shadow: 0 8px 32px rgba(255, 105, 180, 0.3);">
</p>

# 🎀 Cute Desktop Mascot (v1.0.0)

An interactive, transparent desktop mascot for Windows inspired by [nilbuild/page-mascot](https://github.com/nilbuild/page-mascot), featuring your custom character (cute chibi girl with stylish spectacles, wavy dark hair, pearl hairpin, and pastel pink embroidered kurti), hot-swappable character sheets, and playful idle animations.

[![Release](https://img.shields.io/badge/release-v1.0.0-pink.svg)](https://github.com/r2dapps/cute-mascot/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![GitHub Pages](https://img.shields.io/badge/Live%20Showcase-GitHub%20Pages-blue.svg)](https://r2dapps.github.io/cute-mascot/)

---

## ✨ Features

- **🖥️ True Desktop Floating Window**: Native Win32 `UpdateLayeredWindow` with per-pixel alpha transparency. Frameless, zero white background box, always floating on your desktop or over active windows.
- **👀 Global Cursor Tracking**: Head smoothly turns and follows your mouse anywhere across any monitor or application (VS Code, Chrome, games, etc.) at 33–60 FPS.
- **💖 Interactive Boops & Clicks**:
  - Click to boop! She squishes down with rubbery bounce physics and cycles through joyful reactions (hearts, sparkles, blushing).
  - Rapid click 4+ times in a row to make her dizzy with cute swirl eyes and sound cues.
- **🌸 Idle Peeking & Playful Behaviors**:
  - When your mouse is stationary for a few seconds, she playfully interacts on her own!
  - Does natural double-blinks, peeks around with cute winks, glances left and right, and gives playful micro-hops.
- **🎲 Random Disappear & Reappear (Teleportation)**:
  - Every few minutes (or on demand from the menu), she squishes down, disappears, and pops back up at a new random safe spot on your screen with a joyful reaction!
  - Can be toggled on/off or triggered immediately from the right-click menu.
- **✋ Draggable Anywhere**: Left-click and drag her to any position on your desktop.
- **🎭 Hot-Swappable Character Sheets**:
  - Switch characters on the fly directly from the right-click menu!
  - Drop any character folder with `directions.png` and `reactions.png` into `characters/` and it automatically appears in the menu.
  - Automatically remembers your selected character, size, and preferences in `mascot_config.json`.
- **🚀 Run on Windows Startup**: Enable or disable launching automatically when Windows boots up via a simple right-click toggle.
- **⚙️ Native Right-Click Context Menu (Fixed & Always Visible)**:
  - **📏 Size**: Small (140px), Normal (180px), Large (240px), Giant (300px)
  - **🎭 Switch Character ▶**: List of all installed characters + `📂 Open Characters Folder...`
  - **🎲 Teleport Randomly**: Instantly disappear and pop up at a new spot
  - **✨ Auto-Teleport (On / Off)**: Toggle random screen teleportation
  - **🔊 Sound Effects**: Toggle boop sound cues (On / Off)
  - **📌 Always on Top**: Keep mascot above all active windows and taskbars
  - **🚀 Run on Startup**: Toggle automatic Windows startup (via user registry)
  - **📍 Reset to Corner**: Snap back to bottom-right corner
  - **❌ Exit Mascot**: Close the application

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
   **`https://r2dapps.github.io/cute-mascot/`**

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
