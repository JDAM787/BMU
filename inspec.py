"""
inspeccionar_anomalo.py
Abre punch_run5.png y muestra exactamente dónde están los píxeles no transparentes
que están inflando el bounding box. Guarda una versión con el bbox marcado en rojo.
"""
from PIL import Image, ImageDraw
import os

BASE_DIR  = r"C:\Users\germa\OneDrive\Desktop\BMU\app\src\main\assets\personajes\heroe\sprites-personaje"
ARCHIVO   = os.path.join(BASE_DIR, "golpeando", "punch_run5.png")
SALIDA    = os.path.join(BASE_DIR, "golpeando", "punch_run5_DEBUG.png")

img  = Image.open(ARCHIVO).convert("RGBA")
bbox = img.getbbox()
pixels = img.load()

print(f"Tamaño imagen : {img.size}")
print(f"BBox completo : {bbox}  →  contenido {bbox[2]-bbox[0]}×{bbox[3]-bbox[1]} px")
print()

# Buscar columnas con píxeles visibles (alpha > 0) muy a la izquierda o derecha
print("Columnas con píxeles no-transparentes fuera del rango esperado (x < 50 o x > 300):")
columnas_raras = set()
for x in range(img.width):
    for y in range(img.height):
        r, g, b, a = pixels[x, y]
        if a > 10:  # píxel visible
            if x < 50 or x > 300:
                columnas_raras.add(x)

if columnas_raras:
    print(f"  Columnas: {sorted(columnas_raras)}")
    print(f"  → Hay {len(columnas_raras)} columnas con píxeles sueltos fuera del personaje.")
else:
    print("  Ninguna. El problema puede estar en el rango 50-300.")

# Guardar imagen con el bbox marcado en rojo para inspeccionarla visualmente
debug = img.copy()
draw  = ImageDraw.Draw(debug)
draw.rectangle(
    [bbox[0], bbox[1], bbox[2]-1, bbox[3]-1],
    outline=(255, 0, 0, 255),
    width=2
)
# Marcar también el rango "esperado" en verde (~185px de ancho, centrado)
centro_x = img.width // 2
rango_esperado = 185
draw.rectangle(
    [centro_x - rango_esperado//2, bbox[1],
     centro_x + rango_esperado//2, bbox[3]-1],
    outline=(0, 255, 0, 255),
    width=2
)
debug.save(SALIDA, "PNG")
print(f"\nGuardada imagen de debug en: {SALIDA}")
print("  Rojo  = bbox real (lo que está inflando el canvas)")
print("  Verde = rango esperado (~185px centrado)")
print("\nAbre punch_run5_DEBUG.png para ver visualmente el problema.")