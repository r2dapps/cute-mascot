import os
import numpy as np
from PIL import Image

def key_green(img_path, out_path):
    img = Image.open(img_path).convert('RGBA')
    arr = np.array(img, dtype=np.float32)
    r, g, b, a = arr[..., 0], arr[..., 1], arr[..., 2], arr[..., 3]
    
    # Chroma key metric: green dominance over red and blue
    green_diff = g - np.maximum(r, b)
    
    # Soft alpha ramp between 15 and 45
    alpha = np.clip(1.0 - (green_diff - 15.0) / 30.0, 0.0, 1.0)
    
    # Despill / green suppression on the edges
    spill = np.clip((g - np.maximum(r, b)) / 255.0, 0.0, 1.0)
    g_despill = np.minimum(g, np.maximum(r, b) + 5.0)
    g_final = g * (1.0 - spill) + g_despill * spill
    
    arr[..., 1] = g_final
    arr[..., 3] = alpha * 255.0
    
    result = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), mode='RGBA')
    result.save(out_path, 'PNG')
    print(f"Saved {out_path} with transparent background")

if __name__ == '__main__':
    key_green('characters/mascot/directions_raw.png', 'characters/mascot/directions.png')
    key_green('characters/mascot/reactions_raw.png', 'characters/mascot/reactions.png')
