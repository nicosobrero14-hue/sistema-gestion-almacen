"""Dibuja las ilustraciones de los ocho productos de los datos de prueba.

Los datos de prueba son los que se ven en las capturas del documento final. Si se usaran
fotos reales de productos, el documento terminaria mostrando marcas registradas que no
tienen nada que ver con el trabajo. Por eso cada producto arranca con un dibujo plano: se
entiende que es una botella de aceite o un carton de leche, y no es la marca de nadie.

El dibujo sale grande y despareja a proposito. Despues pasa por normalizar-imagen.py, que
es la misma herramienta con la que el comercio va a preparar sus propias fotos: asi queda
probada con algo real y no solo escrita.

Uso:
    python dibujar-ilustraciones.py              (deja los dibujos en crudo/)
    python dibujar-ilustraciones.py ../base-de-datos/imagenes-de-prueba

Necesita Pillow:  pip install Pillow
"""

import sys
from pathlib import Path

from PIL import Image, ImageDraw

LIENZO = 1600
BLANCO = (255, 255, 255)
BORDE = (120, 125, 130)
GROSOR = 5


def lienzo():
    imagen = Image.new("RGB", (LIENZO, LIENZO), BLANCO)
    return imagen, ImageDraw.Draw(imagen)


def caja(dibujo, x0, y0, x1, y1, radio, color):
    dibujo.rounded_rectangle([x0, y0, x1, y1], radio, fill=color, outline=BORDE, width=GROSOR)


def yerba():
    """Paquete de yerba: una bolsa con el borde sellado arriba."""
    imagen, d = lienzo()
    caja(d, 480, 330, 1120, 1290, 40, (58, 110, 62))
    # El sellado de arriba, mas oscuro y un poco mas angosto que la bolsa.
    caja(d, 520, 300, 1080, 400, 18, (40, 84, 44))
    # La banda clara donde iria la etiqueta.
    d.rectangle([480, 700, 1120, 960], fill=(236, 230, 210), outline=BORDE, width=GROSOR)
    # Una calabaza de mate con la bombilla, dibujada con dos formas.
    d.ellipse([720, 760, 900, 920], fill=(120, 78, 44), outline=BORDE, width=GROSOR)
    d.line([880, 790, 960, 700], fill=(150, 150, 155), width=22)
    return imagen


def leche():
    """Carton de leche con el cierre plegado arriba."""
    imagen, d = lienzo()
    d.rectangle([540, 560, 1060, 1300], fill=(250, 250, 252), outline=BORDE, width=GROSOR)
    # El cierre de arriba termina en una cresta plana, no en punta: un carton de leche
    # viene plegado, y con punta parecia el techo de una casa.
    d.polygon([(540, 560), (660, 430), (940, 430), (1060, 560)],
              fill=(238, 240, 246), outline=BORDE)
    d.line([(540, 560), (660, 430), (940, 430), (1060, 560)], fill=BORDE, width=GROSOR)
    d.rectangle([660, 400, 940, 440], fill=(226, 230, 238), outline=BORDE, width=GROSOR)
    # La banda de color y la tapita, sobre la cara de adelante.
    d.rectangle([540, 880, 1060, 1080], fill=(70, 120, 190), outline=BORDE, width=GROSOR)
    d.ellipse([870, 470, 980, 560], fill=(70, 120, 190), outline=BORDE, width=GROSOR)
    return imagen


def fideos():
    """Paquete de fideos con la ventanita transparente."""
    imagen, d = lienzo()
    caja(d, 470, 350, 1130, 1280, 35, (240, 196, 70))
    # La ventana por donde se ven los fideos.
    d.rectangle([570, 620, 1030, 1060], fill=(252, 246, 228), outline=BORDE, width=GROSOR)
    # Unos cuantos mostacholes, apenas curvados.
    for fila, y in enumerate((700, 800, 900, 990)):
        for columna in range(3):
            x = 630 + columna * 130 + (40 if fila % 2 else 0)
            d.arc([x, y, x + 110, y + 70], start=200, end=340, fill=(224, 168, 54), width=26)
    return imagen


def aceite():
    """Botella de aceite: cuerpo, hombros, cuello y tapa."""
    imagen, d = lienzo()
    cuerpo = (610, 700, 990, 1310)
    d.rounded_rectangle(cuerpo, 45, fill=(243, 206, 88), outline=BORDE, width=GROSOR)
    # Los hombros, que llevan del cuerpo al cuello.
    d.polygon([(610, 740), (700, 540), (900, 540), (990, 740)], fill=(243, 206, 88), outline=BORDE)
    d.line([(610, 740), (700, 540), (900, 540), (990, 740)], fill=BORDE, width=GROSOR)
    d.rectangle([700, 420, 900, 560], fill=(243, 206, 88), outline=BORDE, width=GROSOR)
    caja(d, 680, 350, 920, 450, 16, (190, 150, 40))
    # La etiqueta, con el circulo del girasol.
    d.rectangle([610, 900, 990, 1140], fill=(250, 248, 240), outline=BORDE, width=GROSOR)
    d.ellipse([740, 960, 860, 1080], fill=(236, 170, 40), outline=BORDE, width=GROSOR)
    return imagen


def azucar():
    """Bolsa de papel con el doblez de arriba."""
    imagen, d = lienzo()
    # El cuerpo se abre un poco hacia abajo, como una bolsa apoyada y llena.
    d.polygon([(570, 520), (1030, 520), (1070, 1300), (530, 1300)],
              fill=(176, 206, 234), outline=BORDE)
    d.line([(570, 520), (1030, 520), (1070, 1300), (530, 1300), (570, 520)], fill=BORDE, width=GROSOR)
    # El doblez de arriba, bien marcado y mas oscuro que el cuerpo.
    d.polygon([(570, 430), (1030, 430), (1030, 560), (570, 560)],
              fill=(128, 166, 206), outline=BORDE)
    d.line([(570, 560), (1030, 560)], fill=BORDE, width=GROSOR)
    # Los dos pliegues del papel, que son lo que hace leer la forma como una bolsa.
    d.line([(700, 560), (690, 1300)], fill=(140, 174, 210), width=GROSOR)
    d.line([(900, 560), (910, 1300)], fill=(140, 174, 210), width=GROSOR)
    # La etiqueta, donde iria el peso.
    d.rectangle([600, 860, 1000, 1080], fill=(250, 250, 252), outline=BORDE, width=GROSOR)
    return imagen


def galletitas():
    """Paquete de galletitas con tres galletitas a la vista."""
    imagen, d = lienzo()
    caja(d, 440, 480, 1160, 1180, 35, (178, 62, 56))
    # Los bordes sellados de los costados.
    d.rectangle([400, 560, 460, 1100], fill=(156, 50, 46), outline=BORDE, width=GROSOR)
    d.rectangle([1140, 560, 1200, 1100], fill=(156, 50, 46), outline=BORDE, width=GROSOR)
    d.ellipse([540, 660, 1060, 1000], fill=(246, 238, 222), outline=BORDE, width=GROSOR)
    for x in (620, 760, 900):
        d.ellipse([x, 740, x + 120, 860], fill=(198, 150, 92), outline=BORDE, width=GROSOR)
        for punto in (30, 60, 90):
            d.ellipse([x + punto, 780, x + punto + 14, 794], fill=(150, 110, 64))
    return imagen


def gaseosa():
    """Botella de gaseosa cola, con la tapa roja."""
    imagen, d = lienzo()
    d.rounded_rectangle([640, 660, 960, 1330], 50, fill=(92, 52, 30), outline=BORDE, width=GROSOR)
    d.polygon([(640, 700), (710, 480), (890, 480), (960, 700)], fill=(92, 52, 30), outline=BORDE)
    d.line([(640, 700), (710, 480), (890, 480), (960, 700)], fill=BORDE, width=GROSOR)
    d.rectangle([710, 360, 890, 500], fill=(92, 52, 30), outline=BORDE, width=GROSOR)
    caja(d, 690, 290, 910, 390, 16, (190, 56, 48))
    d.rectangle([640, 880, 960, 1110], fill=(222, 56, 48), outline=BORDE, width=GROSOR)
    return imagen


def jabon():
    """Caja de jabon en polvo, con la cara de costado a la vista."""
    imagen, d = lienzo()
    # La cara de adelante.
    d.rectangle([520, 520, 1000, 1240], fill=(62, 118, 176), outline=BORDE, width=GROSOR)
    # El costado y la tapa, para que se lea como una caja y no como un cartel.
    d.polygon([(1000, 520), (1120, 440), (1120, 1160), (1000, 1240)],
              fill=(46, 94, 146), outline=BORDE)
    d.line([(1000, 520), (1120, 440), (1120, 1160), (1000, 1240)], fill=BORDE, width=GROSOR)
    d.polygon([(520, 520), (640, 440), (1120, 440), (1000, 520)],
              fill=(86, 142, 198), outline=BORDE)
    d.line([(520, 520), (640, 440), (1120, 440)], fill=BORDE, width=GROSOR)
    # La banda en diagonal.
    d.polygon([(520, 840), (1000, 740), (1000, 900), (520, 1000)], fill=(250, 250, 252), outline=BORDE)
    return imagen


PRODUCTOS = [
    ("producto-1", yerba),       # Yerba Mate 1kg
    ("producto-2", leche),       # Leche entera 1L
    ("producto-3", fideos),      # Fideos guiseros 500g
    ("producto-4", aceite),      # Aceite girasol 900ml
    ("producto-5", azucar),      # Azucar 1kg
    ("producto-6", galletitas),  # Galletitas surtidas
    ("producto-7", gaseosa),     # Gaseosa cola 2.25L
    ("producto-8", jabon),       # Jabon en polvo 800g
]


def main(argumentos):
    destino = Path(argumentos[0]) if argumentos else Path(__file__).parent / "crudo"
    destino.mkdir(parents=True, exist_ok=True)

    for nombre, dibujar in PRODUCTOS:
        archivo = destino / f"{nombre}.png"
        dibujar().save(archivo)
        print(f"  {archivo.name}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
