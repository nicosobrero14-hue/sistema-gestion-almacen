"""Deja una foto de producto lista para el catalogo: cuadrada, centrada y con fondo blanco.

El sistema no recorta el fondo solo. Eso necesita un modelo de vision y queda fuera del
alcance del trabajo. La foto tiene que entrar ya con fondo claro, que es lo que pasa si se
saca el producto apoyado sobre una hoja blanca o una pared clara.

Lo que si hace este script es lo repetitivo: busca donde termina el producto, le saca el
borde sobrante, lo centra y lo deja de 600x600 sobre blanco. Sin esto, cada foto llega con
un tamanio distinto y la grilla de la pantalla de venta queda despareja.

Uso:
    python normalizar-imagen.py foto.jpg
    python normalizar-imagen.py foto.jpg producto-7.jpg
    python normalizar-imagen.py carpeta-con-fotos/

Necesita Pillow:  pip install Pillow
"""

import sys
from pathlib import Path

from PIL import Image, ImageChops

# 600 alcanza para verse bien en la ficha del producto sin que el archivo pese de mas.
LADO = 600

# Margen alrededor del producto, para que no quede pegado al borde de la tarjeta.
MARGEN = 30

# Cuanto puede alejarse del blanco un pixel y seguir contando como fondo. Una foto nunca
# tiene el fondo blanco puro: la luz de la cocina lo deja grisaceo o amarillento.
TOLERANCIA = 18

EXTENSIONES = {".jpg", ".jpeg", ".png", ".webp", ".bmp"}


def sobre_blanco(imagen):
    """Aplana la imagen sobre blanco. Un PNG con transparencia si no se ve negro."""
    imagen = imagen.convert("RGBA")
    fondo = Image.new("RGBA", imagen.size, (255, 255, 255, 255))
    return Image.alpha_composite(fondo, imagen).convert("RGB")


def recortar_borde(imagen):
    """Devuelve la imagen sin el fondo que sobra alrededor del producto."""
    # La diferencia contra una imagen toda blanca deja el producto y apaga el fondo.
    blanco = Image.new("RGB", imagen.size, (255, 255, 255))
    diferencia = ImageChops.difference(imagen, blanco).convert("L")

    # Todo lo que este por debajo de la tolerancia pasa a ser fondo y no cuenta.
    marcada = diferencia.point(lambda v: 255 if v > TOLERANCIA else 0)

    caja = marcada.getbbox()
    if caja is None:
        # La imagen es toda blanca. Se devuelve como vino: no hay nada que recortar.
        return imagen
    return imagen.crop(caja)


def normalizar(origen, destino):
    with Image.open(origen) as archivo:
        imagen = recortar_borde(sobre_blanco(archivo))

    # Se escala por el lado mas largo, asi el producto entra entero y no se deforma.
    disponible = LADO - MARGEN * 2
    escala = min(disponible / imagen.width, disponible / imagen.height)
    ancho = max(1, round(imagen.width * escala))
    alto = max(1, round(imagen.height * escala))
    imagen = imagen.resize((ancho, alto), Image.LANCZOS)

    lienzo = Image.new("RGB", (LADO, LADO), (255, 255, 255))
    lienzo.paste(imagen, ((LADO - ancho) // 2, (LADO - alto) // 2))

    destino.parent.mkdir(parents=True, exist_ok=True)
    if destino.suffix.lower() in (".jpg", ".jpeg"):
        lienzo.save(destino, quality=88, optimize=True)
    else:
        lienzo.save(destino, optimize=True)
    print(f"  {origen.name} -> {destino.name}  ({destino.stat().st_size // 1024} KB)")


def main(argumentos):
    if not argumentos:
        print(__doc__)
        return 1

    origen = Path(argumentos[0])
    if not origen.exists():
        print(f"No existe {origen}")
        return 1

    if origen.is_dir():
        # Una carpeta entera: cada foto se guarda normalizada en "normalizadas/".
        fotos = sorted(a for a in origen.iterdir() if a.suffix.lower() in EXTENSIONES)
        if not fotos:
            print(f"No hay imagenes en {origen}")
            return 1
        for foto in fotos:
            normalizar(foto, origen / "normalizadas" / foto.name)
        return 0

    destino = Path(argumentos[1]) if len(argumentos) > 1 else origen.with_name(f"{origen.stem}-600{origen.suffix}")
    normalizar(origen, destino)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
