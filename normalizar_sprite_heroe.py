from PIL import Image
import os, glob, sys

# ── Configuración ────────────────────────────────────────────────────────────
BASE_DIR = r"C:\BMU\app\src\main\assets\personajes\heroe\sprites-personaje"
CARPETAS = ["quieto", "caminando", "corriendo", "saltando", "cayendo", "golpeando", "recibeDano"]

# ★ NUEVO: Altura fija para TODOS los personajes (en píxeles, SIN márgenes)
ALTURA_OBJETIVO = 200   # Ajusta este valor según prefieras (ej. 200)

# Margen alrededor del contenido en el canvas final
MARGEN = 20

# ── Paso 1: Buscar PNGs (igual) ──────────────────────────────────────────────
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

# ── Paso 2: Recortar y ESCALAR a altura fija ─────────────────────────────────
print("=" * 60)
print(f"PASO 2 – Recortando y escalando TODOS los frames a altura = {ALTURA_OBJETIVO} px...")
print("=" * 60)

imagenes_escaladas = []   # lista de Image RGBA ya escaladas
rutas_validas       = []

for ruta in rutas:
    try:
        img = Image.open(ruta).convert("RGBA")
        bbox = img.getbbox()

        if bbox is None:
            print(f"  [SKIP] Imagen transparente: {os.path.basename(ruta)}")
            continue

        # Recortar al contenido visible
        recorte = img.crop(bbox)

        # Escalar PROPORCIONALMENTE a la altura objetivo
        proporcion = ALTURA_OBJETIVO / float(recorte.height)
        nuevo_ancho = max(1, int(recorte.width * proporcion))
        recorte_escalado = recorte.resize(
            (nuevo_ancho, ALTURA_OBJETIVO),
            Image.Resampling.NEAREST   # para pixel art
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

# ── Paso 3: Calcular canvas global con márgenes ──────────────────────────────
print()
print("=" * 60)
print("PASO 3 – Calculando canvas normalizado global...")
print("=" * 60)

max_ancho = max(img.width  for img in imagenes_escaladas) + MARGEN * 2
max_alto  = max(img.height for img in imagenes_escaladas) + MARGEN * 2

print(f"  Altura objetivo de personaje: {ALTURA_OBJETIVO} px")
print(f"  Canvas final: {max_ancho} x {max_alto} px (margen {MARGEN} px)")

# ── Paso 4: Pegar en canvas y guardar (alineación por base) ──────────────────
print()
print("=" * 60)
print("PASO 4 – Normalizando y guardando...")
print("=" * 60)

exito   = 0
errores = 0

for img, ruta in zip(imagenes_escaladas, rutas_validas):
    try:
        canvas = Image.new("RGBA", (max_ancho, max_alto), (0, 0, 0, 0))

        # Centrar horizontalmente
        paste_x = (max_ancho - img.width) // 2
        # Alinear por la BASE (pies tocando el mismo nivel)
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
print(f"Todos los personajes tienen la misma altura: {ALTURA_OBJETIVO} px")
print()
print("PRÓXIMO PASO en LibGDX (PantallaJuego.java):")
print(f"  float altoSpriteM  = {max_alto}f / MundoFisico.PPM;")
print(f"  float anchoSpriteM = {max_ancho}f / MundoFisico.PPM;")
print("=" * 60)