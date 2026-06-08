"""
normalizar_sprites_heroe.py
Paso 1: Recorta espacio transparente y reduce frames HD al tamaño pixel-art original.
Paso 2: Calcula un canvas global (el más grande de todos los frames) y lo aplica
        a TODOS los sprites, alineándolos por la base.

Resultado: cada PNG tiene exactamente el mismo tamaño → el personaje en LibGDX
           nunca cambia de tamaño visual al cambiar de animación.
"""

from PIL import Image
import os, glob, sys

# ── Configuración ────────────────────────────────────────────────────────────
BASE_DIR = r"C:\Users\germa\OneDrive\Desktop\BMU\app\src\main\assets\personajes\heroe\sprites-personaje"

# Todas las carpetas que usa AnimadorHeroe.java
CARPETAS = ["quieto", "caminando", "corriendo", "saltando", "cayendo", "golpeando"]

# Altura objetivo para reducir frames HD (en píxeles).
# Se usa solo si un frame tiene más de MAX_ALTO_HD de alto.
ALTURA_PIXEL_ART = 220
MAX_ALTO_HD      = 300   # si el frame es más alto que esto, se reduce

# Margen en píxeles que se añade alrededor del contenido en el canvas final
MARGEN = 20

# ── Paso 1: Recopilar rutas de todos los PNGs ────────────────────────────────
print("=" * 60)
print("PASO 1 – Buscando PNGs en todas las carpetas...")
print("=" * 60)

rutas = []
for carpeta in CARPETAS:
    patron = os.path.join(BASE_DIR, carpeta, "*.png")
    encontrados = sorted(glob.glob(patron))
    if not encontrados:
        print(f"  [AVISO] Ningún PNG encontrado en: {carpeta}/")
    else:
        for r in encontrados:
            rutas.append(r)
            print(f"  OK  {carpeta}/{os.path.basename(r)}")

if not rutas:
    print("\nNo se encontró ningún PNG. Verifica BASE_DIR.")
    sys.exit(1)

print(f"\nTotal de frames encontrados: {len(rutas)}\n")

# ── Paso 2: Leer imágenes, recortar espacio transparente y reducir HD ─────────
print("=" * 60)
print("PASO 2 – Recortando y reduciendo frames HD...")
print("=" * 60)

imagenes_recortadas = []   # lista de Image RGBA ya recortadas
rutas_validas       = []   # rutas correspondientes

for ruta in rutas:
    try:
        img  = Image.open(ruta).convert("RGBA")
        bbox = img.getbbox()

        if bbox is None:
            print(f"  [SKIP] Imagen completamente transparente: {os.path.basename(ruta)}")
            continue

        recorte = img.crop(bbox)

        # Reducir si es HD
        if recorte.height > MAX_ALTO_HD:
            proporcion   = ALTURA_PIXEL_ART / float(recorte.height)
            nuevo_ancho  = max(1, int(recorte.width * proporcion))
            recorte      = recorte.resize(
                (nuevo_ancho, ALTURA_PIXEL_ART),
                Image.Resampling.NEAREST   # preserva el pixel art
            )
            carpeta = os.path.basename(os.path.dirname(ruta))
            print(f"  HD→PX  {carpeta}/{os.path.basename(ruta)}  ({recorte.size})")
        else:
            carpeta = os.path.basename(os.path.dirname(ruta))
            print(f"  CROP   {carpeta}/{os.path.basename(ruta)}  ({recorte.size})")

        imagenes_recortadas.append(recorte)
        rutas_validas.append(ruta)

    except Exception as e:
        print(f"  [ERROR] {os.path.basename(ruta)}: {e}")

if not imagenes_recortadas:
    print("\nNo quedaron imágenes válidas para procesar.")
    sys.exit(1)

# ── Paso 3: Calcular el canvas global ────────────────────────────────────────
print()
print("=" * 60)
print("PASO 3 – Calculando canvas normalizado global...")
print("=" * 60)

max_ancho = max(img.width  for img in imagenes_recortadas) + MARGEN * 2
max_alto  = max(img.height for img in imagenes_recortadas) + MARGEN * 2

print(f"  Canvas final: {max_ancho} x {max_alto} px")
print(f"  (Margen aplicado: {MARGEN}px en cada lado)")

# ── Paso 4: Aplicar canvas a cada frame y guardar ────────────────────────────
print()
print("=" * 60)
print("PASO 4 – Normalizando y guardando todos los frames...")
print("=" * 60)

exito   = 0
errores = 0

for img, ruta in zip(imagenes_recortadas, rutas_validas):
    try:
        canvas = Image.new("RGBA", (max_ancho, max_alto), (0, 0, 0, 0))

        # Centrar horizontalmente
        paste_x = (max_ancho - img.width) // 2

        # Alinear por la BASE (bottom-align):
        # Los pies del personaje siempre quedan en la misma línea Y.
        paste_y = max_alto - img.height - MARGEN

        canvas.paste(img, (paste_x, paste_y))
        canvas.save(ruta, "PNG")

        carpeta = os.path.basename(os.path.dirname(ruta))
        print(f"  OK  {carpeta}/{os.path.basename(ruta)} → pegado en ({paste_x}, {paste_y})")
        exito += 1

    except Exception as e:
        print(f"  [ERROR] {os.path.basename(ruta)}: {e}")
        errores += 1

# ── Resumen ───────────────────────────────────────────────────────────────────
print()
print("=" * 60)
print(f"RESUMEN: {exito} frames normalizados, {errores} errores.")
print(f"Canvas final usado: {max_ancho} x {max_alto} px")
print()
print("PRÓXIMO PASO en LibGDX (PantallaJuego.java):")
print(f"  float altoSpriteM  = {max_alto}f / MundoFisico.PPM;")
print(f"  float anchoSpriteM = {max_ancho}f / MundoFisico.PPM;")
print()
print("Con este tamaño fijo NO necesitas calcular el aspect ratio dinámicamente.")
print("=" * 60)