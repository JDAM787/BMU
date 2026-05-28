from PIL import Image
img = Image.open(r"c:\BMU\app\src\main\assets\personajes\heroe\sprites-personaje\golpeando\golpeando.png")
print(f"Tamaño total: {img.width} x {img.height}")
print(f"Modo: {img.mode}")
# Asumiendo grilla uniforme
cols = 8
rows = 2
fw = img.width // cols
fh = img.height // rows
print(f"Frame size estimado ({cols}x{rows}): {fw} x {fh}")
