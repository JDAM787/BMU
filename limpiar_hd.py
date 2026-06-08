"""
Limpia el espacio transparente de los sprites y reduce las versiones HD
de vuelta a su tamaño pixel-art original para que no consuman tanta memoria RAM.
"""
from PIL import Image
import os
import glob

# Tu directorio base
BASE_DIR = r"C:\Users\germa\OneDrive\Desktop\BMU\app\src\main\assets\personajes\heroe\sprites-personaje"

# Carpetas que tienen el problema de las imágenes gigantes o con mucho espacio en blanco
CARPETAS_A_LIMPIAR = ["saltando", "cayendo"] 

# La altura aproximada de tus sprites originales (stand1.png es 221px)
ALTURA_PIXEL_ART = 220 

print("--- Iniciando limpieza y reducción de Sprites ---")

for carpeta in CARPETAS_A_LIMPIAR:
    ruta = os.path.join(BASE_DIR, carpeta, "*.png")
    
    for archivo in glob.glob(ruta):
        img = Image.open(archivo).convert("RGBA")
        bbox = img.getbbox()
        
        if bbox:
            # 1. Recortar todo el espacio inútil transparente
            recorte = img.crop(bbox)
            
            # 2. Si la imagen es muy grande (HD), la escalamos hacia abajo
            # Usamos NEAREST (Vecino más cercano) para no arruinar el pixel art
            if recorte.height > 400:
                proporcion = ALTURA_PIXEL_ART / float(recorte.height)
                nuevo_ancho = int(float(recorte.width) * proporcion)
                
                # Image.Resampling.NEAREST en Pillow >= 9.0.0
                # Si usas un Pillow viejo y da error, cámbialo por Image.NEAREST
                recorte = recorte.resize((nuevo_ancho, ALTURA_PIXEL_ART), Image.Resampling.NEAREST)
                print(f"Reducido a Pixel Art: {os.path.basename(archivo)}")
            else:
                print(f"Solo recortado: {os.path.basename(archivo)}")

            # 3. Guardar sobrescribiendo el archivo
            recorte.save(archivo, "PNG")
            print(f" -> Guardado con nuevo tamaño: {recorte.size}")

print("\n¡Limpieza terminada! Revisa tus carpetas. Si quieres, también borra la spritesheet completa 'golpeando.png' de la carpeta 'golpeando' si no la usas.")
print("AHORA puedes volver a ejecutar normalizar_run.py (asegúrate de incluir todas las carpetas en su lista).")