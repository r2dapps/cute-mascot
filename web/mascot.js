/**
 * 🎀 Cute Mascot — Romantic Web Companion Engine
 * Includes dual mascot synchronization (Hero Stage + Always-on-Top Corner Mascot),
 * Global vector heart trail, screen click heart burst, and mobile touch tracking.
 */

// --- 3x3 Atlas Grid Coordinates ---
const DIR_COORDS = {
  'up-left':    { x: 0,   y: 0 },
  'up':         { x: 50,  y: 0 },
  'up-right':   { x: 100, y: 0 },
  'left':       { x: 0,   y: 50 },
  'center':     { x: 50,  y: 50 },
  'right':      { x: 100, y: 50 },
  'down-left':  { x: 0,   y: 100 },
  'down':       { x: 50,  y: 100 },
  'down-right': { x: 100, y: 100 }
};

const REACTIONS_COORDS = {
  'blink':      { x: 0,   y: 0 },
  'heart':      { x: 50,  y: 0 },
  'sparkle':    { x: 100, y: 0 },
  'surprised':  { x: 0,   y: 50 },
  'wink':       { x: 50,  y: 50 },
  'bashful':    { x: 100, y: 50 },
  'sleepy':     { x: 0,   y: 100 },
  'dizzy':      { x: 50,  y: 100 },
  'delighted':  { x: 100, y: 100 }
};

const CLOCKWISE = ['right', 'down-right', 'down', 'down-left', 'left', 'up-left', 'up', 'up-right'];
const SECTOR = (Math.PI * 2) / CLOCKWISE.length;
const DEAD_ZONE = 55;
const HYSTERESIS = 0.12;

const SWEET_WHISPERS = [
  "You make my digital heart flutter.",
  "I love watching your cursor glide across the screen.",
  "I am always right here keeping you company.",
  "You are doing wonderful today, keep going.",
  "Every time you click, I feel so appreciated.",
  "Peaceful moments with you are my favorite."
];

// --- Audio Synthesizer (Music Box Chimes) ---
let audioCtx = null;
function getAudioContext() {
  if (!audioCtx) {
    audioCtx = new (window.AudioContext || window.webkitAudioContext)();
  }
  if (audioCtx.state === 'suspended') {
    audioCtx.resume();
  }
  return audioCtx;
}

function playRomanticChime(freq = 587.33, duration = 0.28) {
  try {
    const ctx = getAudioContext();
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();

    osc.type = 'triangle';
    osc.frequency.setValueAtTime(freq, ctx.currentTime);
    osc.frequency.exponentialRampToValueAtTime(freq * 1.04, ctx.currentTime + duration);

    gain.gain.setValueAtTime(0.22, ctx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + duration);

    osc.connect(gain);
    gain.connect(ctx.destination);

    osc.start();
    osc.stop(ctx.currentTime + duration);
  } catch (e) {}
}

function playRomanticArpeggio() {
  const notes = [523.25, 659.25, 783.99, 1046.50]; // C5, E5, G5, C6
  notes.forEach((note, idx) => {
    setTimeout(() => playRomanticChime(note, 0.38), idx * 110);
  });
}

// --- Floating Falling Petals Canvas ---
function initPetalsCanvas() {
  const canvas = document.getElementById('petalsCanvas');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  let w = (canvas.width = window.innerWidth);
  let h = (canvas.height = window.innerHeight);

  window.addEventListener('resize', () => {
    w = canvas.width = window.innerWidth;
    h = canvas.height = window.innerHeight;
  });

  const petals = Array.from({ length: 28 }, () => ({
    x: Math.random() * w,
    y: Math.random() * h,
    size: Math.random() * 8 + 6,
    speedY: Math.random() * 0.7 + 0.5,
    speedX: Math.random() * 0.5 - 0.25,
    rotation: Math.random() * Math.PI * 2,
    rotSpeed: (Math.random() - 0.5) * 0.02,
    opacity: Math.random() * 0.4 + 0.2,
    color: Math.random() > 0.4 ? 'rgba(255, 182, 193, ' : 'rgba(255, 126, 166, '
  }));

  function animate() {
    ctx.clearRect(0, 0, w, h);
    petals.forEach((p) => {
      p.y += p.speedY;
      p.x += Math.sin(p.y * 0.01) * 0.5 + p.speedX;
      p.rotation += p.rotSpeed;

      if (p.y > h + 20) {
        p.y = -20;
        p.x = Math.random() * w;
      }

      ctx.save();
      ctx.translate(p.x, p.y);
      ctx.rotate(p.rotation);
      ctx.fillStyle = `${p.color}${p.opacity})`;
      ctx.beginPath();
      ctx.ellipse(0, 0, p.size, p.size * 0.55, 0, 0, Math.PI * 2);
      ctx.fill();
      ctx.restore();
    });
    requestAnimationFrame(animate);
  }
  animate();
}

// --- Vector Heart Generator (Zero Emojis) ---
function createHeartSVG(color = '#ff7ea6', size = 20) {
  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  svg.setAttribute('viewBox', '0 0 24 24');
  svg.setAttribute('width', size);
  svg.setAttribute('height', size);
  svg.setAttribute('fill', color);
  svg.innerHTML = '<path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>';
  return svg;
}

function spawnHeartBurst(x, y, count = 6) {
  const colors = ['#ff7ea6', '#fca5c5', '#ff94b8', '#ffccd8', '#ffd6a5'];
  for (let i = 0; i < count; i++) {
    const wrap = document.createElement('div');
    wrap.className = 'screen-heart-particle';
    const color = colors[Math.floor(Math.random() * colors.length)];
    const size = Math.floor(Math.random() * 12 + 16);
    wrap.appendChild(createHeartSVG(color, size));

    wrap.style.left = `${x - size / 2}px`;
    wrap.style.top = `${y - size / 2}px`;
    wrap.style.setProperty('--dx', `${Math.random() * 90 - 45}px`);
    wrap.style.setProperty('--dy', `${-(Math.random() * 70 + 40)}px`);
    wrap.style.setProperty('--rot', `${Math.random() * 40 - 20}deg`);

    document.body.appendChild(wrap);
    setTimeout(() => wrap.remove(), 1200);
  }
}

// --- Mouse Trail Hearts ---
let lastTrailTime = 0;
function spawnCursorTrail(x, y) {
  const now = Date.now();
  if (now - lastTrailTime < 80) return;
  lastTrailTime = now;

  const wrap = document.createElement('div');
  wrap.className = 'cursor-trail-heart';
  wrap.appendChild(createHeartSVG('rgba(255, 126, 166, 0.65)', 14));
  wrap.style.left = `${x}px`;
  wrap.style.top = `${y}px`;

  document.body.appendChild(wrap);
  setTimeout(() => wrap.remove(), 800);
}

// --- Dual Mascot Controller ---
class CompanionManager {
  constructor() {
    // Hero Mascot Elements
    this.heroEl = document.getElementById('floatingMascot');
    this.heroSquash = document.getElementById('mascotSquash');
    this.heroDir = document.getElementById('directionsLayer');
    this.heroReact = document.getElementById('reactionsLayer');
    this.heroBubble = document.getElementById('whisperBubble');
    this.heroText = document.getElementById('whisperText');

    // Always-on-Top Corner Mascot Elements
    this.cornerEl = document.getElementById('cornerMascot');
    this.cornerSquash = document.getElementById('cornerSquash');
    this.cornerDir = document.getElementById('cornerDirLayer');
    this.cornerReact = document.getElementById('cornerReactLayer');
    this.cornerBubble = document.getElementById('cornerWhisper');
    this.cornerText = document.getElementById('cornerWhisperText');

    this.currentDirection = 'center';
    this.currentReaction = null;
    this.boopCount = 0;
    this.lastBoopTime = 0;
    this.soundEnabled = true;

    this.init();
  }

  init() {
    this.setDirection('center');

    // 1. Global pointer tracking (Mouse & Touch)
    window.addEventListener('mousemove', (e) => {
      this.handlePointerMove(e.clientX, e.clientY);
      spawnCursorTrail(e.clientX, e.clientY);
    });

    window.addEventListener('touchmove', (e) => {
      if (e.touches.length > 0) {
        this.handlePointerMove(e.touches[0].clientX, e.touches[0].clientY);
      }
    }, { passive: true });

    // 2. Global Screen Click Heart Burst
    window.addEventListener('click', (e) => {
      spawnHeartBurst(e.clientX, e.clientY, 5);
    });

    // 3. Mascot Boop Click Listeners
    this.heroEl?.addEventListener('click', (e) => {
      e.stopPropagation();
      this.triggerBoop(this.heroSquash, e.clientX, e.clientY);
    });

    this.cornerEl?.addEventListener('click', (e) => {
      e.stopPropagation();
      this.triggerBoop(this.cornerSquash, e.clientX, e.clientY, true);
    });

    // 4. Action Buttons
    this.setupActions();

    // 5. Natural Idle Blinks
    setInterval(() => {
      if (!this.currentReaction && Math.random() > 0.4) {
        this.triggerReaction('blink', 350);
      }
    }, 11000);
  }

  calcSectorDirection(el, clientX, clientY) {
    const rect = el.getBoundingClientRect();
    const cx = rect.left + rect.width / 2;
    const cy = rect.top + rect.height / 2;

    const dx = clientX - cx;
    const dy = clientY - cy;
    const dist = Math.hypot(dx, dy);

    if (dist < DEAD_ZONE) {
      return 'center';
    }

    const angle = Math.atan2(dy, dx);
    const sector = Math.round(angle / SECTOR);
    const normalized = (sector + CLOCKWISE.length) % CLOCKWISE.length;
    return CLOCKWISE[normalized];
  }

  handlePointerMove(clientX, clientY) {
    if (this.currentReaction) return;

    // Direct tracking for hero mascot
    const heroDirName = this.calcSectorDirection(this.heroEl, clientX, clientY);
    this.applySpriteDirection(this.heroDir, heroDirName);

    // Direct tracking for corner mascot
    const cornerDirName = this.calcSectorDirection(this.cornerEl, clientX, clientY);
    this.applySpriteDirection(this.cornerDir, cornerDirName);

    this.currentDirection = heroDirName;
  }

  applySpriteDirection(layerEl, name) {
    if (!layerEl) return;
    const coord = DIR_COORDS[name] || DIR_COORDS['center'];
    layerEl.style.backgroundPosition = `${coord.x}% ${coord.y}%`;
  }

  setDirection(name) {
    this.applySpriteDirection(this.heroDir, name);
    this.applySpriteDirection(this.cornerDir, name);
  }

  triggerReaction(name, duration = 650) {
    if (!REACTIONS_COORDS[name]) return;
    this.currentReaction = name;
    const coord = REACTIONS_COORDS[name];

    [this.heroReact, this.cornerReact].forEach(el => {
      if (el) {
        el.style.backgroundPosition = `${coord.x}% ${coord.y}%`;
        el.style.opacity = '1';
      }
    });
    [this.heroDir, this.cornerDir].forEach(el => {
      if (el) el.style.opacity = '0';
    });

    clearTimeout(this.reactionTimeout);
    this.reactionTimeout = setTimeout(() => {
      [this.heroReact, this.cornerReact].forEach(el => {
        if (el) el.style.opacity = '0';
      });
      [this.heroDir, this.cornerDir].forEach(el => {
        if (el) el.style.opacity = '1';
      });
      this.currentReaction = null;
      this.setDirection(this.currentDirection);
    }, duration);
  }

  triggerBoop(squashEl, clickX, clickY, isCorner = false) {
    const now = Date.now();
    if (now - this.lastBoopTime < 1400) {
      this.boopCount++;
    } else {
      this.boopCount = 1;
    }
    this.lastBoopTime = now;

    // Elastic squash keyframe
    if (squashEl) {
      squashEl.style.transform = 'scale(1.22, 0.78)';
      setTimeout(() => {
        squashEl.style.transform = 'scale(0.88, 1.14)';
        setTimeout(() => {
          squashEl.style.transform = 'scale(1.04, 0.96)';
          setTimeout(() => {
            squashEl.style.transform = 'scale(1, 1)';
          }, 110);
        }, 100);
      }, 90);
    }

    // Spawn heart particles
    spawnHeartBurst(clickX, clickY, 8);

    if (this.boopCount >= 4) {
      this.boopCount = 0;
      this.triggerReaction('dizzy', 1400);
      if (this.soundEnabled) {
        playRomanticChime(440, 0.2);
        setTimeout(() => playRomanticChime(350, 0.25), 100);
      }
      this.showBubble("So dizzy... and happy!", isCorner);
    } else {
      const payoffs = ['heart', 'sparkle', 'bashful', 'wink', 'delighted'];
      const pick = payoffs[(this.boopCount - 1) % payoffs.length];
      this.triggerReaction(pick, 750);
      if (this.soundEnabled) {
        const scale = [523.25, 587.33, 659.25, 783.99, 880];
        playRomanticChime(scale[(this.boopCount - 1) % scale.length], 0.22);
      }
    }
  }

  showBubble(text, isCorner = false) {
    const bubble = isCorner ? this.cornerBubble : this.heroBubble;
    const textEl = isCorner ? this.cornerText : this.heroText;
    if (!bubble || !textEl) return;

    textEl.textContent = text;
    bubble.classList.add('visible');
    clearTimeout(bubble.timer);
    bubble.timer = setTimeout(() => {
      bubble.classList.remove('visible');
    }, 3200);
  }

  setupActions() {
    // Send Affection
    document.getElementById('btnRose')?.addEventListener('click', (e) => {
      e.stopPropagation();
      this.triggerReaction('bashful', 1200);
      this.showBubble("A gift for me? Thank you so much.");
      playRomanticChime(659.25, 0.4);
      const rect = e.target.getBoundingClientRect();
      spawnHeartBurst(rect.left + rect.width / 2, rect.top, 8);
    });

    // Whisper Note
    document.getElementById('btnWhisper')?.addEventListener('click', (e) => {
      e.stopPropagation();
      const quote = SWEET_WHISPERS[Math.floor(Math.random() * SWEET_WHISPERS.length)];
      this.triggerReaction('heart', 1100);
      this.showBubble(quote);
      playRomanticChime(587.33, 0.35);
      const rect = e.target.getBoundingClientRect();
      spawnHeartBurst(rect.left + rect.width / 2, rect.top, 5);
    });

    // Boop Cheek
    document.getElementById('btnBoop')?.addEventListener('click', (e) => {
      e.stopPropagation();
      const rect = this.heroEl.getBoundingClientRect();
      this.triggerBoop(this.heroSquash, rect.left + rect.width / 2, rect.top + rect.height / 3);
    });

    // Chime Melody
    document.getElementById('btnMelody')?.addEventListener('click', (e) => {
      e.stopPropagation();
      this.triggerReaction('delighted', 1500);
      playRomanticArpeggio();
      this.showBubble("I love melodies with you.");
      const rect = e.target.getBoundingClientRect();
      spawnHeartBurst(rect.left + rect.width / 2, rect.top, 6);
    });

    // Hero Size Slider
    const sizeRange = document.getElementById('sizeRange');
    const sizeDisplay = document.getElementById('sizeDisplay');
    sizeRange?.addEventListener('input', (e) => {
      const val = e.target.value;
      if (this.heroEl) {
        this.heroEl.style.width = `${val}px`;
        this.heroEl.style.height = `${val}px`;
      }
      if (sizeDisplay) sizeDisplay.textContent = `${val}px`;
    });

    // Sound Switch
    document.getElementById('soundSwitch')?.addEventListener('change', (e) => {
      this.soundEnabled = e.target.checked;
    });

    // Hover Float Switch
    document.getElementById('floatSwitch')?.addEventListener('change', (e) => {
      const checked = e.target.checked;
      [this.heroEl, this.cornerEl].forEach(el => {
        if (el) el.style.animation = checked ? '' : 'none';
      });
    });
  }
}

// Initialize on DOM Ready
window.addEventListener('DOMContentLoaded', () => {
  initPetalsCanvas();
  new CompanionManager();
});
