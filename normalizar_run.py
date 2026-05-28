"""
Normaliza los frames de animacion de carrera para que todos tengan el mismo
tamaño de canvas, con el personaje alineado por la base de los pies.
Esto elimina el efecto de "brincar" causado por frames de diferentes alturas.
"""
from PIL import Image
import os

CARPETA = r"c:\BMU\app\src\main\assets\personajes\heroe\sprites-personaje\corriendo"

frames = ["run1.png", "run2.png", "run3.png", "run4.png", "run5.png", "run6.png"]

# 1. Leer todos los frames y sus bounding boxes
imagenes = []
bboxes = []
for f in frames:
    img = Image.open(os.path.join(CARPETA, f)).convert("RGBA")
    bbox = img.getbbox()  # (left, top, right, bottom)
    imagenes.append(img)
    bboxes.append(bbox)
    print(f"{f}: size={img.size}, bbox={bbox}")

# 2. Calcular el canvas normalizado:
#    - Ancho: el mayor ancho de contenido + margen
#    - Alto: el mayor alto de contenido + margen
#    - Alinear todos por la BASE (bottom del bbox)

max_contenido_ancho = max(b[2] - b[0] for b in bboxes) + 20  # margen lateral
max_contenido_alto  = max(b[3] - b[1] for b in bboxes) + 20  # margen vertical

canvas_w = max_contenido_ancho
canvas_h = max_contenido_alto

print(f"\nCanvas normalizado: {canvas_w}x{canvas_h}")

# 3. Pegar cada frame centrado horizontalmente y alineado por abajo
for i, (img, bbox) in enumerate(zip(imagenes, bboxes)):
    # Recortar solo el contenido visible
    contenido = img.crop(bbox)
    c_w, c_h = contenido.size

    nuevo = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))

    # Centrar horizontalmente
    paste_x = (canvas_w - c_w) // 2
    # Alinear por la base (bottom)
    paste_y = canvas_h - c_h

    nuevo.paste(contenido, (paste_x, paste_y))

    salida = os.path.join(CARPETA, frames[i])
    nuevo.save(salida, "PNG")
    print(f"Guardado {frames[i]}: canvas {canvas_w}x{canvas_h}, personaje en ({paste_x},{paste_y})")

print("\nListo! Todos los frames ahora tienen el mismo tamaño y base alineada.")
