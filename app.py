"""
Native Windows Desktop Mascot (v1.0.0)
Pure Win32 UpdateLayeredWindow implementation with per-pixel alpha transparency.
Zero WebView2 dependency, zero white borders, 60 FPS cursor tracking, draggable and boopable.
Includes Windows Startup toggle, dynamic Character Sheet swapping, and playful idle peeking/teleporting.
"""

import os
import sys
import io
import wave
import struct
import json
import math
import time
import random
import ctypes
import winreg
import threading
import winsound
from ctypes import wintypes
import numpy as np
from PIL import Image

# --- DPI Awareness ---
try:
    ctypes.windll.shcore.SetProcessDpiAwareness(2) # Per-monitor DPI aware
except Exception:
    try:
        ctypes.windll.user32.SetProcessDPIAware()
    except Exception:
        pass

user32 = ctypes.windll.user32
gdi32 = ctypes.windll.gdi32
kernel32 = ctypes.windll.kernel32

# Win32 Constants
WS_POPUP = 0x80000000
WS_VISIBLE = 0x10000000
WS_EX_TOPMOST = 0x00000008
WS_EX_LAYERED = 0x00080000
WS_EX_TOOLWINDOW = 0x00000080

ULW_ALPHA = 0x00000002
AC_SRC_OVER = 0x00
AC_SRC_ALPHA = 0x01
BI_RGB = 0

WM_DESTROY = 0x0002
WM_PAINT = 0x000F
WM_TIMER = 0x0113
WM_LBUTTONDOWN = 0x0201
WM_LBUTTONUP = 0x0202
WM_MOUSEMOVE = 0x0200
WM_RBUTTONUP = 0x0205
WM_COMMAND = 0x0111

MF_STRING = 0x0000
MF_POPUP = 0x0010
MF_CHECKED = 0x0008
MF_UNCHECKED = 0x0000
MF_SEPARATOR = 0x0800
TPM_RIGHTBUTTON = 0x0002
TPM_RETURNCMD = 0x0100
SWP_NOSIZE = 0x0001
SWP_NOMOVE = 0x0002
SWP_NOACTIVATE = 0x0010
HWND_TOPMOST = -1
HWND_NOTOPMOST = -2

# Structs
class POINT(ctypes.Structure):
    _fields_ = [("x", wintypes.LONG), ("y", wintypes.LONG)]

class SIZE(ctypes.Structure):
    _fields_ = [("cx", wintypes.LONG), ("cy", wintypes.LONG)]

class BLENDFUNCTION(ctypes.Structure):
    _fields_ = [
        ("BlendOp", ctypes.c_byte),
        ("BlendFlags", ctypes.c_byte),
        ("SourceConstantAlpha", ctypes.c_byte),
        ("AlphaFormat", ctypes.c_byte)
    ]

class BITMAPINFOHEADER(ctypes.Structure):
    _fields_ = [
        ("biSize", wintypes.DWORD),
        ("biWidth", wintypes.LONG),
        ("biHeight", wintypes.LONG),
        ("biPlanes", wintypes.WORD),
        ("biBitCount", wintypes.WORD),
        ("biCompression", wintypes.DWORD),
        ("biSizeImage", wintypes.DWORD),
        ("biXPelsPerMeter", wintypes.LONG),
        ("biYPelsPerMeter", wintypes.LONG),
        ("biClrUsed", wintypes.DWORD),
        ("biClrImportant", wintypes.DWORD)
    ]

class BITMAPINFO(ctypes.Structure):
    _fields_ = [
        ("bmiHeader", BITMAPINFOHEADER),
        ("bmiColors", wintypes.DWORD * 3)
    ]

# 64-bit Windows API Prototypes
user32.DefWindowProcW.argtypes = [wintypes.HWND, wintypes.UINT, wintypes.WPARAM, wintypes.LPARAM]
user32.DefWindowProcW.restype = wintypes.LPARAM

user32.SetWindowPos.argtypes = [wintypes.HWND, wintypes.HWND, ctypes.c_int, ctypes.c_int, ctypes.c_int, ctypes.c_int, wintypes.UINT]
user32.SetWindowPos.restype = wintypes.BOOL

user32.UpdateLayeredWindow.argtypes = [
    wintypes.HWND,
    wintypes.HDC,
    ctypes.POINTER(POINT),
    ctypes.POINTER(SIZE),
    wintypes.HDC,
    ctypes.POINTER(POINT),
    wintypes.COLORREF,
    ctypes.POINTER(BLENDFUNCTION),
    wintypes.DWORD
]
user32.UpdateLayeredWindow.restype = wintypes.BOOL

user32.CreatePopupMenu.restype = wintypes.HMENU
user32.AppendMenuW.argtypes = [wintypes.HMENU, wintypes.UINT, wintypes.WPARAM, wintypes.LPCWSTR]
user32.AppendMenuW.restype = wintypes.BOOL
user32.DestroyMenu.argtypes = [wintypes.HMENU]
user32.DestroyMenu.restype = wintypes.BOOL
user32.TrackPopupMenu.argtypes = [wintypes.HMENU, wintypes.UINT, ctypes.c_int, ctypes.c_int, ctypes.c_int, wintypes.HWND, ctypes.c_void_p]
user32.TrackPopupMenu.restype = ctypes.c_int
user32.PostMessageW.argtypes = [wintypes.HWND, wintypes.UINT, wintypes.WPARAM, wintypes.LPARAM]
user32.PostMessageW.restype = wintypes.BOOL
user32.BringWindowToTop.argtypes = [wintypes.HWND]
user32.BringWindowToTop.restype = wintypes.BOOL

# Paths Resolution
def get_exe_dir():
    if getattr(sys, 'frozen', False):
        return os.path.dirname(os.path.abspath(sys.executable))
    return os.path.dirname(os.path.abspath(__file__))

EXE_DIR = get_exe_dir()

if getattr(sys, 'frozen', False):
    BUNDLE_DIR = getattr(sys, '_MEIPASS', EXE_DIR)
else:
    BUNDLE_DIR = EXE_DIR

ASSETS_DIR = os.path.join(BUNDLE_DIR, "assets")
if not os.path.exists(ASSETS_DIR):
    ASSETS_DIR = os.path.join(EXE_DIR, "assets")

DEFAULT_DIRECTIONS_PATH = os.path.join(ASSETS_DIR, "mascot-directions.png")
DEFAULT_REACTIONS_PATH = os.path.join(ASSETS_DIR, "mascot-reactions.png")
ICON_PATH = os.path.join(ASSETS_DIR, "icon.ico")
SOUNDS_DIR = os.path.join(ASSETS_DIR, "sounds")
if not os.path.exists(SOUNDS_DIR):
    SOUNDS_DIR = os.path.join(EXE_DIR, "assets", "sounds")
CHARACTERS_DIR = os.path.join(EXE_DIR, "characters")
CONFIG_PATH = os.path.join(EXE_DIR, "mascot_config.json")

# WinMM Multimedia Sound Setup
winmm = ctypes.windll.winmm
SND_ASYNC = 0x0001
SND_NODEFAULT = 0x0002
SND_MEMORY = 0x0004
try:
    winmm.PlaySoundA.argtypes = [ctypes.c_char_p, wintypes.HMODULE, wintypes.DWORD]
    winmm.PlaySoundA.restype = wintypes.BOOL
except Exception:
    pass

class SoundEngine:
    """High-fidelity synthesized and file-backed audio engine matching the website companion."""
    def __init__(self, sounds_dir=None):
        self.sounds_dir = sounds_dir
        self.cache = {}
        self._init_sounds()

    def _synth_multi(self, notes, sample_rate=44100, volume=0.35):
        total_duration = max(st + dur for st, freq, dur in notes)
        num_samples = int(sample_rate * total_duration)
        buffer = [0.0] * num_samples
        for st, freq, dur in notes:
            start_idx = int(st * sample_rate)
            note_samples = int(dur * sample_rate)
            phase = 0.0
            decay = 5.4 / dur
            for i in range(note_samples):
                idx = start_idx + i
                if idx >= num_samples:
                    break
                t = i / sample_rate
                f = freq * (1.04 ** (t / dur))
                phase += 2.0 * math.pi * f / sample_rate
                env = volume * math.exp(-t * decay)
                if i >= note_samples - 100:
                    env *= (note_samples - 1 - i) / 100.0
                p = (phase / (2.0 * math.pi)) % 1.0
                tri = 4.0 * abs(p - 0.5) - 1.0
                buffer[idx] += env * tri
        frames = bytearray()
        for s in buffer:
            val = int(max(-1.0, min(1.0, s)) * 32767.0)
            frames += struct.pack('<h', val)
        buf = io.BytesIO()
        with wave.open(buf, 'wb') as wf:
            wf.setnchannels(1)
            wf.setsampwidth(2)
            wf.setframerate(sample_rate)
            wf.writeframes(frames)
        return buf.getvalue()

    def _init_sounds(self):
        scale = [523.25, 587.33, 659.25, 783.99, 880.00]
        for i, freq in enumerate(scale):
            key = f"boop_{i+1}"
            path = os.path.join(self.sounds_dir, f"{key}.wav") if self.sounds_dir else None
            if path and os.path.isfile(path):
                try:
                    with open(path, "rb") as f:
                        self.cache[key] = f.read()
                except Exception:
                    self.cache[key] = self._synth_multi([(0.0, freq, 0.26)])
            else:
                self.cache[key] = self._synth_multi([(0.0, freq, 0.26)])

        # Dizzy sound (440Hz -> 350Hz)
        dizzy_path = os.path.join(self.sounds_dir, "dizzy.wav") if self.sounds_dir else None
        if dizzy_path and os.path.isfile(dizzy_path):
            try:
                with open(dizzy_path, "rb") as f:
                    self.cache["dizzy"] = f.read()
            except Exception:
                self.cache["dizzy"] = self._synth_multi([(0.0, 440.0, 0.20), (0.10, 350.0, 0.25)])
        else:
            self.cache["dizzy"] = self._synth_multi([(0.0, 440.0, 0.20), (0.10, 350.0, 0.25)])

        # Teleport sound (chime arpeggio)
        tele_path = os.path.join(self.sounds_dir, "teleport.wav") if self.sounds_dir else None
        if tele_path and os.path.isfile(tele_path):
            try:
                with open(tele_path, "rb") as f:
                    self.cache["teleport"] = f.read()
            except Exception:
                self.cache["teleport"] = self._synth_multi([(0.0, 523.25, 0.20), (0.09, 783.99, 0.22), (0.18, 1046.50, 0.30)])
        else:
            self.cache["teleport"] = self._synth_multi([(0.0, 523.25, 0.20), (0.09, 783.99, 0.22), (0.18, 1046.50, 0.30)])

        # Sparkle sound (fairy crystal shimmer)
        sparkle_path = os.path.join(self.sounds_dir, "sparkle.wav") if self.sounds_dir else None
        if sparkle_path and os.path.isfile(sparkle_path):
            try:
                with open(sparkle_path, "rb") as f:
                    self.cache["sparkle"] = f.read()
            except Exception:
                self.cache["sparkle"] = self._synth_multi([(0.00, 880.0, 0.18), (0.06, 1174.66, 0.18), (0.12, 1396.91, 0.20), (0.18, 1760.0, 0.25)])
        else:
            self.cache["sparkle"] = self._synth_multi([(0.00, 880.0, 0.18), (0.06, 1174.66, 0.18), (0.12, 1396.91, 0.20), (0.18, 1760.0, 0.25)])

        # Blush sound (warm flutter)
        blush_path = os.path.join(self.sounds_dir, "blush.wav") if self.sounds_dir else None
        if blush_path and os.path.isfile(blush_path):
            try:
                with open(blush_path, "rb") as f:
                    self.cache["blush"] = f.read()
            except Exception:
                self.cache["blush"] = self._synth_multi([(0.00, 622.25, 0.18), (0.09, 739.99, 0.28)])
        else:
            self.cache["blush"] = self._synth_multi([(0.00, 622.25, 0.18), (0.09, 739.99, 0.28)])

        # Sleepy sound (soothing lullaby)
        sleepy_path = os.path.join(self.sounds_dir, "sleepy.wav") if self.sounds_dir else None
        if sleepy_path and os.path.isfile(sleepy_path):
            try:
                with open(sleepy_path, "rb") as f:
                    self.cache["sleepy"] = f.read()
            except Exception:
                self.cache["sleepy"] = self._synth_multi([(0.00, 523.25, 0.25), (0.14, 392.00, 0.35)])
        else:
            self.cache["sleepy"] = self._synth_multi([(0.00, 523.25, 0.25), (0.14, 392.00, 0.35)])

    def play(self, sound_name):
        data = self.cache.get(sound_name)
        if data:
            try:
                winmm.PlaySoundA(data, 0, SND_MEMORY | SND_ASYNC | SND_NODEFAULT)
            except Exception:
                pass

REG_RUN_KEY = r"Software\Microsoft\Windows\CurrentVersion\Run"
APP_REG_NAME = "CuteDesktopMascot"

# Direction and reaction indexes
DIR_NAMES = ['up-left', 'up', 'up-right', 'left', 'center', 'right', 'down-left', 'down', 'down-right']
REACTIONS_NAMES = ['blink', 'heart', 'sparkle', 'surprised', 'wink', 'bashful', 'sleepy', 'dizzy', 'delighted']
PAYOFFS = ['heart', 'sparkle', 'delighted', 'bashful', 'wink']

# Clockwise angle mapping: right (0), down-right, down, down-left, left, up-left, up, up-right
CLOCKWISE = ['right', 'down-right', 'down', 'down-left', 'left', 'up-left', 'up', 'up-right']
SECTOR = (math.pi * 2) / len(CLOCKWISE)
DEAD_ZONE = 60
HYSTERESIS = 0.12

WNDPROC = ctypes.WINFUNCTYPE(ctypes.c_longlong, wintypes.HWND, wintypes.UINT, wintypes.WPARAM, wintypes.LPARAM)

class WNDCLASSEX(ctypes.Structure):
    _fields_ = [
        ("cbSize", wintypes.UINT),
        ("style", wintypes.UINT),
        ("lpfnWndProc", WNDPROC),
        ("cbClsExtra", ctypes.c_int),
        ("cbWndExtra", ctypes.c_int),
        ("hInstance", wintypes.HINSTANCE),
        ("hIcon", wintypes.HICON),
        ("hCursor", wintypes.HICON),
        ("hbrBackground", wintypes.HBRUSH),
        ("lpszMenuName", wintypes.LPCWSTR),
        ("lpszClassName", wintypes.LPCWSTR),
        ("hIconSm", wintypes.HICON)
    ]

# Registry Startup Helpers
def is_startup_enabled():
    try:
        key = winreg.OpenKey(winreg.HKEY_CURRENT_USER, REG_RUN_KEY, 0, winreg.KEY_READ)
        val, _ = winreg.QueryValueEx(key, APP_REG_NAME)
        winreg.CloseKey(key)
        return bool(val)
    except Exception:
        return False

def set_startup_enabled(enable: bool):
    try:
        key = winreg.OpenKey(winreg.HKEY_CURRENT_USER, REG_RUN_KEY, 0, winreg.KEY_SET_VALUE)
        if enable:
            if getattr(sys, 'frozen', False):
                cmd = f'"{os.path.abspath(sys.executable)}"'
            else:
                cmd = f'"{os.path.abspath(sys.executable)}" "{os.path.abspath(__file__)}"'
            winreg.SetValueEx(key, APP_REG_NAME, 0, winreg.REG_SZ, cmd)
        else:
            try:
                winreg.DeleteValue(key, APP_REG_NAME)
            except FileNotFoundError:
                pass
        winreg.CloseKey(key)
        return True
    except Exception as e:
        print("Set startup error:", e)
        return False

# Character Discovery
def discover_all_characters():
    custom_chars = [
        {
            "id": "mascot",
            "name": "Mascot (Cute Girl)",
            "dir_path": DEFAULT_DIRECTIONS_PATH,
            "react_path": DEFAULT_REACTIONS_PATH
        }
    ]
    seen_ids = {"mascot"}

    # 1. Custom characters folder
    if os.path.isdir(CHARACTERS_DIR):
        for entry in sorted(os.listdir(CHARACTERS_DIR)):
            folder = os.path.join(CHARACTERS_DIR, entry)
            if os.path.isdir(folder) and entry.lower() not in seen_ids:
                d_file = os.path.join(folder, "directions.png")
                r_file = os.path.join(folder, "reactions.png")
                if os.path.isfile(d_file):
                    name = entry.replace("-", " ").replace("_", " ").title()
                    custom_chars.append({
                        "id": entry,
                        "name": name,
                        "dir_path": d_file,
                        "react_path": r_file if os.path.isfile(r_file) else None
                    })
                    seen_ids.add(entry.lower())

    # 2. Local reference repository characters (if present on disk)
    ref_chars = []
    ref_dir = os.path.join(EXE_DIR, "reference-page-mascot", "characters")
    if os.path.isdir(ref_dir):
        for entry in sorted(os.listdir(ref_dir)):
            folder = os.path.join(ref_dir, entry)
            if os.path.isdir(folder) and entry.lower() not in seen_ids:
                d_file = os.path.join(folder, "directions.png")
                r_file = os.path.join(folder, "reactions.png")
                if os.path.isfile(d_file):
                    name = entry.replace("-", " ").replace("_", " ").title()
                    ref_chars.append({
                        "id": f"ref_{entry}",
                        "name": name,
                        "dir_path": d_file,
                        "react_path": r_file if os.path.isfile(r_file) else None
                    })
                    seen_ids.add(entry.lower())

    return custom_chars, ref_chars

class NativeMascot:
    def __init__(self):
        self.size = 180
        self.direction = 'center'
        self.reaction = None
        self.sector = -1
        self.sound_enabled = True
        self.sound_engine = SoundEngine(SOUNDS_DIR)
        self.always_on_top = True
        self.teleport_enabled = True
        self.last_topmost_check = 0
        self.current_char_id = "mascot"
        self.current_char_name = "Mascot (Cute Girl)"
        
        # Menu state (prevents z-order fighting during right-click)
        self.menu_active = False
        
        # Boop tracking
        self.boop_count = 0
        self.last_boop_time = 0
        self.reaction_until = 0
        self.squash_until = 0
        self.squash_scale_y = 1.0
        self.squash_scale_x = 1.0
        
        # Idle & Peeking
        now = time.time()
        self.last_cursor_pos = (0, 0)
        self.last_mouse_move_time = now
        self.next_idle_anim_time = now + 4.5
        
        # Random Teleport (Disappear & Reappear)
        self.next_teleport_time = now + random.uniform(50.0, 95.0)
        self.teleport_state = 0 # 0: normal, 1: shrinking away, 2: popping in
        self.teleport_until = 0
        self.teleport_target_pos = None
        
        # Dragging
        self.is_dragging = False
        self.drag_start_x = 0
        self.drag_start_y = 0
        self.win_start_x = 0
        self.win_start_y = 0
        
        self.hwnd = None
        self.dir_tiles = {}
        self.react_tiles = {}

        self.load_config()
        self.init_character()

    def load_config(self):
        try:
            if os.path.isfile(CONFIG_PATH):
                with open(CONFIG_PATH, "r", encoding="utf-8") as f:
                    data = json.load(f)
                    self.size = data.get("size", 180)
                    self.sound_enabled = data.get("sound_enabled", True)
                    self.always_on_top = data.get("always_on_top", True)
                    self.teleport_enabled = data.get("teleport_enabled", True)
                    self.current_char_id = data.get("character", "mascot")
        except Exception as e:
            print("Config load error:", e)

    def save_config(self):
        try:
            data = {
                "character": self.current_char_id,
                "size": self.size,
                "sound_enabled": self.sound_enabled,
                "always_on_top": self.always_on_top,
                "teleport_enabled": self.teleport_enabled
            }
            with open(CONFIG_PATH, "w", encoding="utf-8") as f:
                json.dump(data, f, indent=2)
        except Exception as e:
            print("Config save error:", e)

    def init_character(self):
        custom_chars, ref_chars = discover_all_characters()
        all_chars = custom_chars + ref_chars
        matched = next((c for c in all_chars if c["id"] == self.current_char_id), None)
        if not matched:
            matched = custom_chars[0]
        self.load_character_data(matched)

    def load_character_data(self, char_info):
        """Slice the 3x3 sheets and pre-compute premultiplied BGRA buffers."""
        dir_path = char_info["dir_path"]
        react_path = char_info.get("react_path")
        
        try:
            dir_img = Image.open(dir_path).convert('RGBA')
        except Exception as e:
            print(f"Error opening directions from {dir_path}: {e}")
            dir_img = Image.open(DEFAULT_DIRECTIONS_PATH).convert('RGBA')
        
        dw, dh = dir_img.size
        cw, ch = dw // 3, dh // 3
        
        new_dir_tiles = {}
        for r in range(3):
            for c in range(3):
                name = DIR_NAMES[r * 3 + c]
                new_dir_tiles[name] = dir_img.crop((c * cw, r * ch, (c + 1) * cw, (r + 1) * ch))
                
        new_react_tiles = {}
        if react_path and os.path.isfile(react_path):
            try:
                react_img = Image.open(react_path).convert('RGBA')
                rw, rh = react_img.size
                rcw, rch = rw // 3, rh // 3
                for r in range(3):
                    for c in range(3):
                        name = REACTIONS_NAMES[r * 3 + c]
                        new_react_tiles[name] = react_img.crop((c * rcw, r * rch, (c + 1) * rcw, (r + 1) * rch))
            except Exception as e:
                print(f"Error opening reactions: {e}")
        
        # Fallback missing reaction frames to neutral center frame
        for rname in REACTIONS_NAMES:
            if rname not in new_react_tiles:
                new_react_tiles[rname] = new_dir_tiles.get('center')
                
        self.dir_tiles = new_dir_tiles
        self.react_tiles = new_react_tiles
        self.current_char_id = char_info["id"]
        self.current_char_name = char_info["name"]
        self.save_config()
        if self.hwnd:
            self.update_window_bitmap()

    def get_current_image(self):
        if self.reaction and self.reaction in self.react_tiles:
            base_img = self.react_tiles[self.reaction]
        else:
            base_img = self.dir_tiles.get(self.direction, self.dir_tiles.get('center'))
            
        # Apply current size and squash/stretch transform
        target_w = max(4, int(round(self.size * self.squash_scale_x)))
        target_h = max(4, int(round(self.size * self.squash_scale_y)))
        
        resized = base_img.resize((target_w, target_h), Image.Resampling.LANCZOS)
        
        # Place into fixed self.size x self.size canvas anchored at bottom
        canvas = Image.new('RGBA', (self.size, self.size), (0, 0, 0, 0))
        offset_x = (self.size - target_w) // 2
        offset_y = self.size - target_h
        canvas.paste(resized, (offset_x, offset_y), resized)
        return canvas

    def update_window_bitmap(self):
        if not self.hwnd or self.menu_active:
            return
            
        img = self.get_current_image()
        w, h = self.size, self.size
        
        # Convert RGBA to premultiplied BGRA
        arr = np.array(img, dtype=np.float32)
        alpha = arr[..., 3] / 255.0
        b = np.clip(arr[..., 2] * alpha, 0, 255).astype(np.uint8)
        g = np.clip(arr[..., 1] * alpha, 0, 255).astype(np.uint8)
        r = np.clip(arr[..., 0] * alpha, 0, 255).astype(np.uint8)
        a = arr[..., 3].astype(np.uint8)
        
        bgra = np.dstack([b, g, r, a])
        # Win32 DIB is bottom-to-top unless height is negative
        bgra_flipped = np.ascontiguousarray(np.flipud(bgra)).tobytes()
        
        hdc_screen = user32.GetDC(0)
        hdc_mem = gdi32.CreateCompatibleDC(hdc_screen)
        
        bmi = BITMAPINFO()
        bmi.bmiHeader.biSize = ctypes.sizeof(BITMAPINFOHEADER)
        bmi.bmiHeader.biWidth = w
        bmi.bmiHeader.biHeight = h
        bmi.bmiHeader.biPlanes = 1
        bmi.bmiHeader.biBitCount = 32
        bmi.bmiHeader.biCompression = BI_RGB
        
        ppv_bits = ctypes.c_void_p()
        hbmp = gdi32.CreateDIBSection(hdc_screen, ctypes.byref(bmi), 0, ctypes.byref(ppv_bits), None, 0)
        
        ctypes.memmove(ppv_bits, bgra_flipped, len(bgra_flipped))
        hbmp_old = gdi32.SelectObject(hdc_mem, hbmp)
        
        # Get window current pos
        rect = wintypes.RECT()
        user32.GetWindowRect(self.hwnd, ctypes.byref(rect))
        
        pt_dest = POINT(rect.left, rect.top)
        sz = SIZE(w, h)
        pt_src = POINT(0, 0)
        
        blend = BLENDFUNCTION()
        blend.BlendOp = AC_SRC_OVER
        blend.BlendFlags = 0
        blend.SourceConstantAlpha = 255
        blend.AlphaFormat = AC_SRC_ALPHA
        
        user32.UpdateLayeredWindow(
            self.hwnd,
            hdc_screen,
            ctypes.byref(pt_dest),
            ctypes.byref(sz),
            hdc_mem,
            ctypes.byref(pt_src),
            0,
            ctypes.byref(blend),
            ULW_ALPHA
        )
        
        gdi32.SelectObject(hdc_mem, hbmp_old)
        gdi32.DeleteObject(hbmp)
        gdi32.DeleteDC(hdc_mem)
        user32.ReleaseDC(0, hdc_screen)

    def trigger_boop(self):
        now = time.time()
        if now - self.last_boop_time < 1.6:
            self.boop_count += 1
        else:
            self.boop_count = 1
        self.last_boop_time = now
        
        if self.boop_count >= 4:
            self.boop_count = 0
            self.reaction = 'dizzy'
            self.reaction_until = now + 1.2
            if self.sound_enabled:
                self.sound_engine.play('dizzy')
        else:
            payoff = PAYOFFS[(self.boop_count - 1) % len(PAYOFFS)]
            self.reaction = payoff
            self.reaction_until = now + 0.65
            if self.sound_enabled:
                if payoff == 'sparkle':
                    self.sound_engine.play('sparkle')
                elif payoff == 'bashful':
                    self.sound_engine.play('blush')
                else:
                    boop_idx = ((self.boop_count - 1) % 5) + 1
                    self.sound_engine.play(f'boop_{boop_idx}')
                
        # Squash animation timing
        self.squash_until = now + 0.40

    def trigger_teleport(self):
        """Initiates a playful disappear and reappear sequence at a random screen location."""
        now = time.time()
        sw = user32.GetSystemMetrics(0)
        sh = user32.GetSystemMetrics(1)
        
        # Pick a random safe target coordinate on the user's desktop
        target_x = random.randint(60, max(100, sw - self.size - 60))
        target_y = random.randint(60, max(100, sh - self.size - 80))
        self.teleport_target_pos = (target_x, target_y)
        
        self.teleport_state = 1 # Disappearing
        self.teleport_until = now + 0.35
        self.reaction = 'surprised'
        self.reaction_until = now + 0.35

    def tick(self):
        # Never update or fight for z-order while the user is interacting with the right-click menu
        if self.menu_active:
            return
            
        now = time.time()
        need_update = False
        
        # Check reaction expiry
        if self.reaction and now > self.reaction_until:
            self.reaction = None
            need_update = True

        # --- Teleport Animation States ---
        if self.teleport_state == 1:
            # Stage 1: Shrink/squash down and disappear
            progress = 1.0 - max(0.0, (self.teleport_until - now) / 0.35)
            self.squash_scale_y = max(0.05, 1.0 - progress * 0.95)
            self.squash_scale_x = 1.0 + progress * 0.4
            need_update = True
            if now >= self.teleport_until:
                # Move window to new random spot instantly
                new_x, new_y = self.teleport_target_pos
                hwnd_top = HWND_TOPMOST if self.always_on_top else HWND_NOTOPMOST
                user32.SetWindowPos(self.hwnd, hwnd_top, new_x, new_y, self.size, self.size, SWP_NOACTIVATE)
                
                # Switch to Stage 2: Pop up with random payoff reaction
                self.teleport_state = 2
                self.teleport_until = now + 0.45
                self.reaction = random.choice(PAYOFFS)
                self.reaction_until = now + 1.2
                if self.sound_enabled:
                    self.sound_engine.play('teleport')
        elif self.teleport_state == 2:
            # Stage 2: Pop back up with joyful elastic bounce
            progress = 1.0 - max(0.0, (self.teleport_until - now) / 0.45)
            if progress < 0.4:
                p = progress / 0.4
                self.squash_scale_y = 0.2 + 1.0 * p
                self.squash_scale_x = 1.3 - 0.4 * p
            elif progress < 0.75:
                p = (progress - 0.4) / 0.35
                self.squash_scale_y = 1.2 - 0.25 * p
                self.squash_scale_x = 0.9 + 0.15 * p
            else:
                self.squash_scale_y = 1.0
                self.squash_scale_x = 1.0
            need_update = True
            if now >= self.teleport_until:
                self.teleport_state = 0
                self.squash_scale_y = 1.0
                self.squash_scale_x = 1.0
                self.next_teleport_time = now + random.uniform(50.0, 95.0)
        else:
            # Standard squash/bounce animation frames
            if now < self.squash_until:
                progress = 1.0 - (self.squash_until - now) / 0.40
                if progress < 0.25:
                    p = progress / 0.25
                    self.squash_scale_y = 1.0 - 0.16 * p
                    self.squash_scale_x = 1.0 + 0.12 * p
                elif progress < 0.60:
                    p = (progress - 0.25) / 0.35
                    self.squash_scale_y = 0.84 + 0.22 * p
                    self.squash_scale_x = 1.12 - 0.16 * p
                else:
                    p = (progress - 0.60) / 0.40
                    self.squash_scale_y = 1.06 - 0.06 * p
                    self.squash_scale_x = 0.96 + 0.04 * p
                need_update = True
            else:
                if self.squash_scale_y != 1.0 or self.squash_scale_x != 1.0:
                    self.squash_scale_y = 1.0
                    self.squash_scale_x = 1.0
                    need_update = True

        # Random Auto-Teleport Trigger
        if self.teleport_enabled and self.teleport_state == 0 and not self.is_dragging:
            if now > self.next_teleport_time:
                self.trigger_teleport()
                return

        # Cursor Tracking & Idle Natural Reactions
        if not self.is_dragging and self.teleport_state == 0:
            pt = POINT()
            user32.GetCursorPos(ctypes.byref(pt))
            
            # Check mouse movement
            if (pt.x, pt.y) != self.last_cursor_pos:
                self.last_cursor_pos = (pt.x, pt.y)
                self.last_mouse_move_time = now
                self.next_idle_anim_time = now + random.uniform(10.0, 20.0)
                is_idle = False
            else:
                is_idle = (now - self.last_mouse_move_time > 5.0)

            # Idle Natural Reactions (strictly facial reactions: gentle blink or wink, spaced out)
            # NOTE: Directions are strictly driven by cursor position, NEVER randomized.
            if is_idle and not self.reaction and now > self.next_idle_anim_time:
                self.next_idle_anim_time = now + random.uniform(12.0, 24.0)
                idle_action = random.choice(['blink', 'blink', 'wink', 'bashful'])
                if idle_action == 'blink':
                    self.reaction = 'blink'
                    self.reaction_until = now + 0.32
                elif idle_action == 'wink':
                    self.reaction = 'wink'
                    self.reaction_until = now + 0.50
                elif idle_action == 'bashful':
                    self.reaction = 'bashful'
                    self.reaction_until = now + 0.65
                    if self.sound_enabled:
                        self.sound_engine.play('blush')
                need_update = True

            # Tracking calculations (always tracks cursor accurately across 360 degrees)
            rect = wintypes.RECT()
            user32.GetWindowRect(self.hwnd, ctypes.byref(rect))
            
            cx = rect.left + self.size // 2
            cy = rect.top + self.size // 2
            
            dx = pt.x - cx
            dy = pt.y - cy
            dist = math.hypot(dx, dy)
            
            if dist < DEAD_ZONE:
                new_dir = 'center'
                self.sector = -1
            else:
                angle = math.atan2(dy, dx)
                if self.sector != -1:
                    diff = math.atan2(math.sin(angle - self.sector * SECTOR),
                                      math.cos(angle - self.sector * SECTOR))
                    if abs(diff) < (SECTOR / 2 + HYSTERESIS):
                        new_dir = self.direction
                    else:
                        sector = int((round(angle / SECTOR) + len(CLOCKWISE)) % len(CLOCKWISE))
                        self.sector = sector
                        new_dir = CLOCKWISE[sector]
                else:
                    sector = int((round(angle / SECTOR) + len(CLOCKWISE)) % len(CLOCKWISE))
                    self.sector = sector
                    new_dir = CLOCKWISE[sector]

            if new_dir != self.direction:
                self.direction = new_dir
                need_update = True
                
        # Re-assert Always on Top periodically so taskbar never covers the mascot
        if self.always_on_top and (now - self.last_topmost_check > 0.5):
            self.last_topmost_check = now
            user32.SetWindowPos(self.hwnd, HWND_TOPMOST, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_NOACTIVATE)
                
        if need_update:
            self.update_window_bitmap()

    def set_size(self, new_size):
        self.size = new_size
        rect = wintypes.RECT()
        user32.GetWindowRect(self.hwnd, ctypes.byref(rect))
        hwnd_top = HWND_TOPMOST if self.always_on_top else HWND_NOTOPMOST
        user32.SetWindowPos(self.hwnd, hwnd_top, rect.left, rect.top, self.size, self.size, SWP_NOACTIVATE)
        self.save_config()
        self.update_window_bitmap()

    def show_context_menu(self):
        self.menu_active = True
        hmenu = user32.CreatePopupMenu()
        
        try:
            # 1. Sizes
            user32.AppendMenuW(hmenu, MF_STRING, 101, "📏 Small (140px)")
            user32.AppendMenuW(hmenu, MF_STRING, 102, "📏 Normal (180px)")
            user32.AppendMenuW(hmenu, MF_STRING, 103, "📏 Large (240px)")
            user32.AppendMenuW(hmenu, MF_STRING, 104, "📏 Giant (300px)")
            user32.AppendMenuW(hmenu, MF_SEPARATOR, 0, None)
            
            # 2. Character Swapping Submenus
            custom_chars, ref_chars = discover_all_characters()
            all_selectable = custom_chars + ref_chars
            
            h_char_menu = user32.CreatePopupMenu()
            for idx, c in enumerate(custom_chars):
                flag = MF_CHECKED if c["id"] == self.current_char_id else MF_UNCHECKED
                user32.AppendMenuW(h_char_menu, MF_STRING | flag, 1000 + idx, c["name"])
                
            if ref_chars:
                h_ref_menu = user32.CreatePopupMenu()
                base_offset = len(custom_chars)
                for idx, c in enumerate(ref_chars):
                    flag = MF_CHECKED if c["id"] == self.current_char_id else MF_UNCHECKED
                    user32.AppendMenuW(h_ref_menu, MF_STRING | flag, 1000 + base_offset + idx, c["name"])
                user32.AppendMenuW(h_char_menu, MF_SEPARATOR, 0, None)
                user32.AppendMenuW(h_char_menu, MF_POPUP, h_ref_menu, "📚 Reference Characters")
                
            user32.AppendMenuW(h_char_menu, MF_SEPARATOR, 0, None)
            user32.AppendMenuW(h_char_menu, MF_STRING, 205, "📂 Open Characters Folder...")
            
            user32.AppendMenuW(hmenu, MF_POPUP, h_char_menu, "🎭 Switch Character")
            user32.AppendMenuW(hmenu, MF_SEPARATOR, 0, None)
            
            # 3. Teleport controls
            user32.AppendMenuW(hmenu, MF_STRING, 206, "🎲 Teleport Randomly")
            tp_flag = MF_CHECKED if self.teleport_enabled else MF_UNCHECKED
            user32.AppendMenuW(hmenu, MF_STRING | tp_flag, 207, "✨ Auto-Teleport (Every Few Mins)")
            user32.AppendMenuW(hmenu, MF_SEPARATOR, 0, None)
            
            # 4. Toggles
            sound_flag = MF_CHECKED if self.sound_enabled else MF_UNCHECKED
            user32.AppendMenuW(hmenu, MF_STRING | sound_flag, 201, "🔊 Sound Effects")
            
            top_flag = MF_CHECKED if self.always_on_top else MF_UNCHECKED
            user32.AppendMenuW(hmenu, MF_STRING | top_flag, 202, "📌 Always on Top")
            
            startup_flag = MF_CHECKED if is_startup_enabled() else MF_UNCHECKED
            user32.AppendMenuW(hmenu, MF_STRING | startup_flag, 204, "🚀 Run on Startup")
            
            # 5. Reset to Corner
            user32.AppendMenuW(hmenu, MF_STRING, 203, "📍 Reset to Corner")
            user32.AppendMenuW(hmenu, MF_SEPARATOR, 0, None)
            
            # 6. Exit
            user32.AppendMenuW(hmenu, MF_STRING, 999, "❌ Exit Mascot")
            
            pt = POINT()
            user32.GetCursorPos(ctypes.byref(pt))
            user32.SetForegroundWindow(self.hwnd)
            user32.BringWindowToTop(self.hwnd)
            
            # TrackPopupMenu flags: TPM_RIGHTBUTTON | TPM_RETURNCMD
            cmd = user32.TrackPopupMenu(hmenu, TPM_RIGHTBUTTON | TPM_RETURNCMD, pt.x, pt.y, 0, self.hwnd, None)
            user32.PostMessageW(self.hwnd, 0, 0, 0)
        finally:
            user32.DestroyMenu(hmenu)
            self.menu_active = False
        
        if cmd == 101: self.set_size(140)
        elif cmd == 102: self.set_size(180)
        elif cmd == 103: self.set_size(240)
        elif cmd == 104: self.set_size(300)
        elif cmd == 201:
            self.sound_enabled = not self.sound_enabled
            if self.sound_enabled:
                self.sound_engine.play('boop_3')
            self.save_config()
        elif cmd == 202:
            self.always_on_top = not self.always_on_top
            hwnd_top = HWND_TOPMOST if self.always_on_top else HWND_NOTOPMOST
            user32.SetWindowPos(self.hwnd, hwnd_top, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_NOACTIVATE)
            self.save_config()
        elif cmd == 203:
            sw = user32.GetSystemMetrics(0)
            sh = user32.GetSystemMetrics(1)
            hwnd_top = HWND_TOPMOST if self.always_on_top else HWND_NOTOPMOST
            user32.SetWindowPos(self.hwnd, hwnd_top, sw - self.size - 40, sh - self.size - 80, self.size, self.size, SWP_NOACTIVATE)
        elif cmd == 204:
            cur = is_startup_enabled()
            set_startup_enabled(not cur)
        elif cmd == 205:
            os.makedirs(CHARACTERS_DIR, exist_ok=True)
            try:
                os.startfile(CHARACTERS_DIR)
            except Exception:
                import subprocess
                subprocess.Popen(["explorer", CHARACTERS_DIR])
        elif cmd == 206:
            self.trigger_teleport()
        elif cmd == 207:
            self.teleport_enabled = not self.teleport_enabled
            self.save_config()
        elif 1000 <= cmd < 1000 + len(all_selectable):
            selected = all_selectable[cmd - 1000]
            self.load_character_data(selected)
        elif cmd == 999:
            user32.PostQuitMessage(0)

# Global reference
mascot_app = None

def wnd_proc(hwnd, msg, wparam, lparam):
    global mascot_app
    if msg == WM_TIMER:
        if mascot_app:
            mascot_app.tick()
        return 0
    elif msg == WM_LBUTTONDOWN:
        if mascot_app:
            mascot_app.is_dragging = False
            pt = POINT()
            user32.GetCursorPos(ctypes.byref(pt))
            mascot_app.drag_start_x = pt.x
            mascot_app.drag_start_y = pt.y
            rect = wintypes.RECT()
            user32.GetWindowRect(hwnd, ctypes.byref(rect))
            mascot_app.win_start_x = rect.left
            mascot_app.win_start_y = rect.top
            user32.SetCapture(hwnd)
        return 0
    elif msg == WM_MOUSEMOVE:
        if mascot_app and user32.GetCapture() == hwnd:
            pt = POINT()
            user32.GetCursorPos(ctypes.byref(pt))
            dx = pt.x - mascot_app.drag_start_x
            dy = pt.y - mascot_app.drag_start_y
            if math.hypot(dx, dy) > 8:
                mascot_app.is_dragging = True
                hwnd_top = HWND_TOPMOST if mascot_app.always_on_top else HWND_NOTOPMOST
                user32.SetWindowPos(
                    hwnd, hwnd_top,
                    mascot_app.win_start_x + dx,
                    mascot_app.win_start_y + dy,
                    0, 0,
                    SWP_NOSIZE | SWP_NOACTIVATE
                )
        return 0
    elif msg == WM_LBUTTONUP:
        if mascot_app:
            user32.ReleaseCapture()
            if not mascot_app.is_dragging:
                mascot_app.trigger_boop()
            mascot_app.is_dragging = False
        return 0
    elif msg == WM_RBUTTONUP:
        if mascot_app:
            mascot_app.show_context_menu()
        return 0
    elif msg == WM_DESTROY:
        user32.PostQuitMessage(0)
        return 0
    return user32.DefWindowProcW(hwnd, msg, wparam, lparam)

def main():
    global mascot_app
    mascot_app = NativeMascot()
    
    hinst = kernel32.GetModuleHandleW(None)
    cls_name = "NativeDesktopMascotClass"
    
    wndclass = WNDCLASSEX()
    wndclass.cbSize = ctypes.sizeof(WNDCLASSEX)
    wndclass.style = 0
    wndclass.lpfnWndProc = WNDPROC(wnd_proc)
    wndclass.cbClsExtra = 0
    wndclass.cbWndExtra = 0
    wndclass.hInstance = hinst
    wndclass.hIcon = 0
    wndclass.hCursor = user32.LoadCursorW(0, 32512) # IDC_ARROW
    wndclass.hbrBackground = 0
    wndclass.lpszMenuName = None
    wndclass.lpszClassName = cls_name
    wndclass.hIconSm = 0
    
    user32.RegisterClassExW(ctypes.byref(wndclass))
    
    screen_w = user32.GetSystemMetrics(0)
    screen_h = user32.GetSystemMetrics(1)
    init_x = screen_w - mascot_app.size - 40
    init_y = screen_h - mascot_app.size - 80
    
    ex_style = WS_EX_TOPMOST | WS_EX_LAYERED | WS_EX_TOOLWINDOW
    
    hwnd = user32.CreateWindowExW(
        ex_style,
        cls_name,
        "Desktop Mascot",
        WS_POPUP | WS_VISIBLE,
        init_x,
        init_y,
        mascot_app.size,
        mascot_app.size,
        0, 0, hinst, None
    )
    
    mascot_app.hwnd = hwnd
    mascot_app.update_window_bitmap()
    
    # 30ms timer (~33 FPS)
    user32.SetTimer(hwnd, 1, 30, None)
    
    msg = wintypes.MSG()
    while user32.GetMessageW(ctypes.byref(msg), 0, 0, 0) != 0:
        user32.TranslateMessage(ctypes.byref(msg))
        user32.DispatchMessageW(ctypes.byref(msg))

if __name__ == '__main__':
    main()
