from PIL import Image
import os, glob, sys

# ── Configuración ────────────────────────────────────────────────────────────
BASE_DIR = r"C:\Users\germa\OneDrive\Desktop\BMU\app\src\main\assets\personajes\EnemigoDebil\EnemigoDebil1"
CARPETAS_NORMALES   = ["quieto", "caminando", "RecibeDano", "corriendo", "golpeando"]
CARPETAS_ESPECIALES = ["muerto"]  # canvas propio, no afecta el tamaño global

ALTURA_OBJETIVO = 200
MARGEN = 20

# ── Paso 1: Buscar PNGs ──────────────────────────────────────────────────────
print("=" * 60)
print("PASO 1 – Buscando PNGs en todas las carpetas...")
print("=" * 60)

rutas = []
# ✅ FIX: iterar sobre la unión de ambas listas
for carpeta in CARPETAS_NORMALES + CARPETAS_ESPECIALES:
    patron = os.path.join(BASE_DIR, carpeta, "*.png")
    encontrados = sorted(glob.glob(patron))
    if not encontrados:
        print(f"  [AVISO] Ningún PNG encontrado en: {carpeta}/")
    else:
        for r in encontrados:
            rutas.append(r)
            print(f"  OK  {carpeta}/{os.path.basename(r)}")

if not rutas:
    print("\nNo se encontró ningún PNG. Verifica BASE_DIR y los nombres de las carpetas.")
    sys.exit(1)

print(f"\nTotal de frames encontrados: {len(rutas)}\n")

# ── Paso 2: Recortar y escalar a altura fija ─────────────────────────────────
print("=" * 60)
print(f"PASO 2 – Recortando y escalando TODOS los frames a altura = {ALTURA_OBJETIVO} px...")
print("=" * 60)

imagenes_escaladas = []
rutas_validas = []

for ruta in rutas:
    try:
        img = Image.open(ruta).convert("RGBA")
        bbox = img.getbbox()

        if bbox is None:
            print(f"  [SKIP] Imagen transparente: {os.path.basename(ruta)}")
            continue

        recorte = img.crop(bbox)

        proporcion = ALTURA_OBJETIVO / float(recorte.height)
        nuevo_ancho = max(1, int(recorte.width * proporcion))
        recorte_escalado = recorte.resize(
            (nuevo_ancho, ALTURA_OBJETIVO),
            Image.Resampling.NEAREST
        )

        carpeta = os.path.basename(os.path.dirname(ruta))
        print(f"  {carpeta}/{os.path.basename(ruta):20} original: {recorte.size} → escalado: {recorte_escalado.size}")

        imagenes_escaladas.append(recorte_escalado)
        rutas_validas.append(ruta)

    except Exception as e:
        print(f"  [ERROR] {os.path.basename(ruta)}: {e}")

if not imagenes_escaladas:
    print("\nNo quedaron imágenes válidas.")
    sys.exit(1)

# ── Paso 3: Separar y calcular canvas ────────────────────────────────────────
print()
print("=" * 60)
print("PASO 3 – Calculando canvas normalizado global...")
print("=" * 60)

imagenes_normales   = []
rutas_normales      = []
imagenes_especiales = []
rutas_especiales    = []

for img, ruta in zip(imagenes_escaladas, rutas_validas):
    carpeta = os.path.basename(os.path.dirname(ruta))
    if carpeta in CARPETAS_ESPECIALES:
        imagenes_especiales.append(img)
        rutas_especiales.append(ruta)
    else:
        imagenes_normales.append(img)
        rutas_normales.append(ruta)

max_ancho = max(img.width for img in imagenes_normales) + MARGEN * 2
max_alto  = max(img.height for img in imagenes_normales) + MARGEN * 2
print(f"  Canvas normal: {max_ancho} x {max_alto} px")

max_ancho_muerto = max(img.width for img in imagenes_especiales) + MARGEN * 2 if imagenes_especiales else 0
max_alto_muerto  = max(img.height for img in imagenes_especiales) + MARGEN * 2 if imagenes_especiales else 0
if imagenes_especiales:
    print(f"  Canvas muerto: {max_ancho_muerto} x {max_alto_muerto} px")

# ── Paso 4: Guardar ───────────────────────────────────────────────────────────
print()
print("=" * 60)
print("PASO 4 – Normalizando y guardando...")
print("=" * 60)

# ✅ FIX: definir contadores antes de usarlos
exito   = 0
errores = 0

# Guardar frames normales
for img, ruta in zip(imagenes_normales, rutas_normales):
    try:
        canvas = Image.new("RGBA", (max_ancho, max_alto), (0, 0, 0, 0))
        paste_x = (max_ancho - img.width) // 2
        paste_y = max_alto - img.height - MARGEN
        canvas.paste(img, (paste_x, paste_y))
        canvas.save(ruta, "PNG")
        print(f"  OK  {os.path.basename(os.path.dirname(ruta))}/{os.path.basename(ruta)}")
        exito += 1
    except Exception as e:
        print(f"  [ERROR] {os.path.basename(ruta)}: {e}")
        errores += 1

# Guardar muerto con su propio canvas
for img, ruta in zip(imagenes_especiales, rutas_especiales):
    try:
        canvas = Image.new("RGBA", (max_ancho_muerto, max_alto_muerto), (0, 0, 0, 0))
        paste_x = (max_ancho_muerto - img.width) // 2
        paste_y = max_alto_muerto - img.height - MARGEN
        canvas.paste(img, (paste_x, paste_y))
        canvas.save(ruta, "PNG")
        print(f"  OK  muerto/{os.path.basename(ruta)} (canvas propio)")
        exito += 1
    except Exception as e:
        print(f"  [ERROR] {os.path.basename(ruta)}: {e}")
        errores += 1

# ── Resumen ───────────────────────────────────────────────────────────────────
print()
print("=" * 60)
print(f"RESUMEN: {exito} frames normalizados, {errores} errores.")
print(f"Canvas normal usado: {max_ancho} x {max_alto} px")
print(f"Todos los personajes tienen la misma altura: {ALTURA_OBJETIVO} px")
print()
print("PRÓXIMO PASO en LibGDX (PantallaJuego.java):")
print(f"  Para el enemigo débil, usa en su AnimadorEnemigoDebil:")
print(f"  altoSpriteM  = {max_alto}f / MundoFisico.PPM;")
print(f"  anchoSpriteM = {max_ancho}f / MundoFisico.PPM;")
print("=" * 60)