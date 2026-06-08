"""
diagnostico_sprites.py
Muestra el tamaño del CONTENIDO VISIBLE de cada frame (sin transparencia)
para detectar frames anómalos antes de normalizar.
"""
from PIL import Image
import os, glob

BASE_DIR = r"C:\Users\germa\OneDrive\Desktop\BMU\app\src\main\assets\personajes\heroe\sprites-personaje"
CARPETAS = ["quieto", "caminando", "corriendo", "saltando", "cayendo", "golpeando"]

print(f"{'Carpeta':<12} {'Archivo':<20} {'Tamaño img':>12} {'Contenido (w×h)':>18} {'Ancho':>7} {'Alto':>7}")
print("-" * 80)

max_ancho = 0
max_alto  = 0
anomalos  = []

for carpeta in CARPETAS:
    for ruta in sorted(glob.glob(os.path.join(BASE_DIR, carpeta, "*.png"))):
        img  = Image.open(ruta).convert("RGBA")
        bbox = img.getbbox()
        nombre = os.path.basename(ruta)
        
        if bbox is None:
            print(f"{carpeta:<12} {nombre:<20} {'(vacía)':>12}")
            continue
        
        w = bbox[2] - bbox[0]
        h = bbox[3] - bbox[1]
        
        if w > max_ancho: max_ancho = w
        if h > max_alto:  max_alto  = h
        
        # Marcar frames que son >50% más anchos que el promedio de quieto (~91px)
        alerta = " ← ANÓMALO" if w > 200 else ""
        if alerta:
            anomalos.append((carpeta, nombre, w, h))
        
        print(f"{carpeta:<12} {nombre:<20} {str(img.size):>12}   {w:>6}×{h:<6}  {w:>6}  {h:>6}{alerta}")

print("-" * 80)
print(f"\nContenido más grande encontrado: {max_ancho} × {max_alto} px")
print(f"Canvas final estimado (+ 40px margen): {max_ancho+40} × {max_alto+40} px")

if anomalos:
    print(f"\n⚠  Frames anómalos que van a inflar el canvas:")
    for carpeta, nombre, w, h in anomalos:
        print(f"   {carpeta}/{nombre}  →  {w}×{h} px de contenido")
    print("\n   Revísalos manualmente antes de normalizar.")
else:
    print("\nTodos los frames tienen un ancho razonable. Puedes normalizar.")