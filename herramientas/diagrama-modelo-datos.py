# -*- coding: utf-8 -*-
# Dibuja el diagrama entidad-relacion de la base.
#
# Se corre desde la raiz del repositorio:  python herramientas/diagrama-modelo-datos.py
#
# Las tablas y las relaciones se leen del esquema real, no se transcriben: una version
# anterior de este diagrama, escrita a mano, tenia cuatro columnas de menos y una que
# no existe. Leyendo el SQL eso no puede volver a pasar.

import io
import re

from PIL import Image, ImageDraw, ImageFont

ESQUEMA = "base-de-datos/01-esquema.sql"
SALIDA = "documentacion/imagenes/modelo-datos.png"

ESCALA = 3
TEXTO = (23, 34, 45)
GRIS = (110, 122, 132)
BORDE = (168, 176, 170)
LINEA = (120, 130, 125)
FONDO = (255, 255, 255)
MARFIL = (250, 251, 249)
NARANJA = (150, 90, 30)
ROJO = (170, 60, 50)
AZUL_GRIS = (90, 110, 125)

# Un color por grupo de tablas, como en el diagrama que ya estaba en el documento.
GRUPOS = {
    "Catálogo": ((172, 203, 227), ["proveedores", "productos"]),
    "Usuarios": ((201, 184, 232), ["usuarios"]),
    "Stock": ((242, 193, 138), ["movimientos_stock"]),
    "Ventas y pagos": ((168, 213, 162), ["ventas", "detalle_venta", "pagos"]),
    "Compras": ((159, 211, 206), ["compras", "pagos_proveedor"]),
}
COLOR_DE = {t: color for color, tablas in GRUPOS.values() for t in tablas}


def fuente(tam, negrita=False):
    nombre = "C:/Windows/Fonts/" + ("seguisb.ttf" if negrita else "segoeui.ttf")
    try:
        return ImageFont.truetype(nombre, int(tam * ESCALA))
    except OSError:
        return ImageFont.truetype("C:/Windows/Fonts/arial.ttf", int(tam * ESCALA))


# ------------------------------------------------------------------ el modelo
def leer_esquema(ruta):
    sql = re.sub(r"--[^\n]*", "", io.open(ruta, encoding="utf-8").read())
    tablas, claves = {}, {}
    patron = re.compile(r"CREATE TABLE IF NOT EXISTS (\w+)\s*\((.*?)\n\)\s*ENGINE", re.S)
    reservadas = ("PRIMARY", "FOREIGN", "CONSTRAINT", "UNIQUE", "KEY", "INDEX", "CHECK", "ON")
    for m in patron.finditer(sql):
        tabla, cuerpo = m.group(1), m.group(2)
        pk = re.search(r"PRIMARY KEY \((\w+)\)", cuerpo)
        fks = dict(re.findall(r"FOREIGN KEY \((\w+)\) REFERENCES (\w+)", cuerpo))
        # Solo las de una columna llevan marca. La unica compuesta, la que impide cargar
        # dos veces la misma factura de un proveedor, se explica en el texto.
        unicas = set(c.strip() for c in re.findall(r"UNIQUE \(([^)]+)\)", cuerpo)
                     if "," not in c)
        columnas = []
        for linea in cuerpo.split("\n"):
            linea = linea.strip().rstrip(",")
            partes = linea.split()
            if len(partes) < 2 or not re.match(r"^[a-z_]+$", partes[0]):
                continue
            if partes[0].upper() in reservadas:
                continue
            columnas.append({
                "nombre": partes[0],
                "tipo": partes[1],
                "clase": "pk" if pk and partes[0] == pk.group(1) else (
                    "fk" if partes[0] in fks else ""),
                "opcional": "NOT NULL" not in linea.upper(),
                "unica": partes[0] in unicas,
            })
        tablas[tabla] = columnas
        claves[tabla] = fks
    return tablas, claves


TABLAS, CLAVES = leer_esquema(ESQUEMA)
# (origen, campo, destino): el origen tiene muchos, el destino uno.
RELACIONES = [(o, c, d) for o, fks in CLAVES.items() for c, d in fks.items()]

# Columna y fila de cada tabla. Las tres mas altas comparten fila: asi el alto total
# lo fija una sola y el dibujo entra en la pagina con la letra legible.
POSICION = {
    "proveedores": (0, 0), "usuarios": (1, 0), "ventas": (2, 0),
    "productos": (0, 1), "compras": (1, 1), "movimientos_stock": (2, 1),
    "detalle_venta": (0, 2), "pagos_proveedor": (1, 2), "pagos": (2, 2),
}

GUTTER_X = 64
GUTTER_Y = 52
MARGEN = 36
ALTO_TITULO = 22
ALTO_FILA = 15
ALTO_ENCABEZADO = 34
ALTO_LEYENDA = 32

f_doc = fuente(13, True)
f_doc_sub = fuente(7.5)
f_titulo = fuente(9, True)
f_campo = fuente(8)
f_tipo = fuente(6.5)
f_clave = fuente(6, True)
f_rel = fuente(6)
f_leyenda = fuente(6.5)

_medidor = ImageDraw.Draw(Image.new("RGB", (8, 8)))


def ancho_de(texto, f):
    return _medidor.textlength(texto, font=f) / ESCALA


def _ancho_necesario():
    # El ancho sale de la fila mas larga: marca + nombre + hueco + tipo.
    # La marca de valor unico va entre el nombre y el tipo: si no se cuenta, en la fila
    # mas larga se monta encima del nombre.
    filas = max(ancho_de(c["nombre"], f_campo) + ancho_de(c["tipo"], f_tipo)
                + (15 if c["unica"] else 0)
                for cs in TABLAS.values() for c in cs)
    titulos = max(ancho_de(t, f_titulo) for t in TABLAS)
    return int(max(22 + filas + 16, 10 + titulos + 10)) + 1


ANCHO_CAJA = _ancho_necesario()


def alto(tabla):
    return ALTO_TITULO + len(TABLAS[tabla]) * ALTO_FILA + 6


filas_por_numero = {}
for t, (col, fila) in POSICION.items():
    filas_por_numero.setdefault(fila, []).append(t)
y_fila = {}
y = MARGEN + ALTO_ENCABEZADO
for f in sorted(filas_por_numero):
    y_fila[f] = y
    y += max(alto(t) for t in filas_por_numero[f]) + GUTTER_Y

ANCHO = MARGEN * 2 + ANCHO_CAJA * 3 + GUTTER_X * 2
FIN_CAJAS = y - GUTTER_Y
ALTO = FIN_CAJAS + ALTO_LEYENDA + MARGEN


def caja(tabla):
    col, fila = POSICION[tabla]
    return MARGEN + col * (ANCHO_CAJA + GUTTER_X), y_fila[fila], ANCHO_CAJA, alto(tabla)


img = Image.new("RGB", (ANCHO * ESCALA, ALTO * ESCALA), FONDO)
d = ImageDraw.Draw(img)


def e(v):
    return v * ESCALA


def sobre_caja(px, py, margen=6):
    for t in TABLAS:
        bx, by, bw, bh = caja(t)
        if bx - margen < px < bx + bw + margen and by - margen < py < by + bh + margen:
            return True
    return False


# ------------------------------------------------------------------ relaciones
# Ruteo en angulo recto: la linea sale por un costado, corre por una canaleta vertical
# libre y entra por el costado de la otra tabla. Con doce relaciones, las diagonales
# se cruzaban entre si y atravesaban las cajas.
CANALETA = {i + 0.5: MARGEN + (i + 1) * ANCHO_CAJA + i * GUTTER_X + GUTTER_X / 2
            for i in range(2)}
CANALETA[-0.5] = MARGEN / 2
CANALETA[2.5] = ANCHO - MARGEN / 2

usos = {}


def carril(clave, paso):
    n = usos.get(clave, 0)
    usos[clave] = n + 1
    return (0, 1, -1, 2, -2, 3, -3)[n % 7] * paso


def lado(tabla, hacia_derecha, desplazamiento):
    x, y_, w, h = caja(tabla)
    cy = min(max(y_ + h / 2 + desplazamiento, y_ + ALTO_TITULO + 8), y_ + h - 8)
    return ((x + w) if hacia_derecha else x, cy)


def pata_de_gallo(p, hacia):
    px, py = p
    dx, dy = hacia[0] - px, hacia[1] - py
    largo = max((dx * dx + dy * dy) ** 0.5, 0.001)
    dx, dy = dx / largo, dy / largo
    nx, ny = -dy, dx
    vertice = (px + dx * 11, py + dy * 11)
    for k in (-1, 0, 1):
        d.line([e(vertice[0]), e(vertice[1]), e(px + nx * k * 5), e(py + ny * k * 5)],
               fill=LINEA, width=ESCALA)


def barra_uno(p, hacia):
    px, py = p
    dx, dy = hacia[0] - px, hacia[1] - py
    largo = max((dx * dx + dy * dy) ** 0.5, 0.001)
    dx, dy = dx / largo, dy / largo
    nx, ny = -dy, dx
    d.line([e(px + dx * 10 - nx * 5), e(py + dy * 10 - ny * 5),
            e(px + dx * 10 + nx * 5), e(py + dy * 10 + ny * 5)], fill=LINEA, width=ESCALA)


for origen, campo, destino in RELACIONES:
    col_o, fila_o = POSICION[origen]
    col_d, fila_d = POSICION[destino]
    if col_o == col_d and abs(fila_o - fila_d) == 1:
        xo, yo, wo, ho = caja(origen)
        xd, yd, wd, hd = caja(destino)
        x = xo + wo / 2 + carril(("v", col_o, min(fila_o, fila_d)), 20)
        camino = [(x, yo if fila_o > fila_d else yo + ho),
                  (x, yd + hd if fila_o > fila_d else yd)]
    else:
        if col_o == col_d:
            llave = (col_o - 0.5) if col_o == 0 else (col_o + 0.5)
        else:
            llave = (col_o + col_d) / 2
            if llave not in CANALETA:
                llave = (col_o + 0.5) if col_d > col_o else (col_o - 0.5)
        xg = CANALETA[llave] + carril(("g", llave), 15)
        der_o = xg > caja(origen)[0]
        der_d = xg > caja(destino)[0]
        p1 = lado(origen, der_o, carril(("l", origen, der_o), 18))
        p2 = lado(destino, der_d, carril(("l", destino, der_d), 18))
        camino = [p1, (xg, p1[1]), (xg, p2[1]), p2]

    for a, b in zip(camino, camino[1:]):
        d.line([e(a[0]), e(a[1]), e(b[0]), e(b[1])], fill=LINEA, width=ESCALA)
    pata_de_gallo(camino[0], camino[1])
    barra_uno(camino[-1], camino[-2])

    # Nombre del campo, en un punto del recorrido que no caiga sobre ninguna caja.
    mx = my = None
    for a, b in sorted(zip(camino, camino[1:]),
                       key=lambda t: -(abs(t[0][0] - t[1][0]) + abs(t[0][1] - t[1][1]))):
        for k in (0.5, 0.35, 0.65, 0.25, 0.75):
            cx, cy = a[0] + (b[0] - a[0]) * k, a[1] + (b[1] - a[1]) * k
            if not sobre_caja(cx, cy):
                mx, my = cx, cy
                break
        if mx is not None:
            break
    if mx is None:
        continue
    ancho_t = ancho_de(campo, f_rel)
    d.rectangle([e(mx - ancho_t / 2 - 3), e(my - 5), e(mx + ancho_t / 2 + 3), e(my + 5)],
                fill=FONDO)
    d.text((e(mx - ancho_t / 2), e(my - 4)), campo, font=f_rel, fill=GRIS)


# ------------------------------------------------------------------ cajas
def marca(x, y, opcional, color):
    # Rombo lleno si la columna es obligatoria, hueco si admite nulo.
    puntos = [(e(x + 4), e(y)), (e(x + 8), e(y + 4)), (e(x + 4), e(y + 8)), (e(x), e(y + 4))]
    d.polygon(puntos, fill=FONDO if opcional else color, outline=color)


for tabla, campos in TABLAS.items():
    x, y, w, h = caja(tabla)
    color = COLOR_DE[tabla]
    d.rectangle([e(x), e(y), e(x + w), e(y + h)], fill=MARFIL, outline=BORDE, width=ESCALA)
    d.rectangle([e(x), e(y), e(x + w), e(y + ALTO_TITULO)], fill=color, outline=BORDE,
                width=ESCALA)
    d.text((e(x + 8), e(y + 5)), tabla, font=f_titulo, fill=TEXTO)
    cy = y + ALTO_TITULO + 4
    for c in campos:
        if c["clase"]:
            d.text((e(x + 5), e(cy + 2)), c["clase"].upper(), font=f_clave,
                   fill=NARANJA if c["clase"] == "pk" else ROJO)
        else:
            marca(x + 6, cy + 3, c["opcional"], AZUL_GRIS)
        d.text((e(x + 22), e(cy)), c["nombre"], font=f_campo, fill=TEXTO)
        ancho_tipo = ancho_de(c["tipo"], f_tipo)
        if c["unica"]:
            d.text((e(x + w - 8 - ancho_tipo - 13), e(cy + 2)), "UQ", font=f_clave, fill=NARANJA)
        d.text((e(x + w - 8 - ancho_tipo), e(cy + 1)), c["tipo"], font=f_tipo, fill=GRIS)
        if c["clase"] == "pk":
            ancho_n = ancho_de(c["nombre"], f_campo)
            d.line([e(x + 22), e(cy) + f_campo.size + e(1),
                    e(x + 22 + ancho_n), e(cy) + f_campo.size + e(1)], fill=TEXTO, width=1)
        cy += ALTO_FILA

# ------------------------------------------------------------------ titulo y leyenda
d.text((e(MARGEN), e(MARGEN - 18)), "Modelo de datos", font=f_doc, fill=TEXTO)
d.text((e(MARGEN), e(MARGEN + 2)),
       f"MySQL 8 · motor InnoDB · utf8mb4 · {len(TABLAS)} tablas · {len(RELACIONES)} relaciones",
       font=f_doc_sub, fill=GRIS)

gx = ANCHO - MARGEN
for nombre, (color, _) in reversed(list(GRUPOS.items())):
    ancho_g = ancho_de(nombre, f_leyenda) + 14
    gx -= ancho_g + 6
    d.rounded_rectangle([e(gx), e(MARGEN - 14), e(gx + ancho_g), e(MARGEN - 1)],
                        radius=e(6), fill=color, outline=color)
    d.text((e(gx + 7), e(MARGEN - 12)), nombre, font=f_leyenda, fill=TEXTO)

ly = FIN_CAJAS + 14
d.line([e(MARGEN), e(ly - 6), e(ANCHO - MARGEN), e(ly - 6)], fill=BORDE, width=ESCALA)
lx = MARGEN
for texto, dibujo in [("Clave primaria", "pk"), ("Clave foránea", "fk"),
                      ("Obligatoria", "llena"), ("Opcional (NULL)", "hueca"),
                      ("Valor único", "uq"), ("Uno", "uno"), ("Muchos", "muchos")]:
    if dibujo in ("pk", "fk", "uq"):
        d.text((e(lx), e(ly + 2)), {"pk": "PK", "fk": "FK", "uq": "UQ"}[dibujo],
               font=f_clave, fill=ROJO if dibujo == "fk" else NARANJA)
        lx += 15
    elif dibujo in ("llena", "hueca"):
        marca(lx, ly + 2, dibujo == "hueca", AZUL_GRIS)
        lx += 14
    elif dibujo == "uno":
        d.line([e(lx), e(ly + 6), e(lx + 14), e(ly + 6)], fill=LINEA, width=ESCALA)
        d.line([e(lx + 9), e(ly + 2), e(lx + 9), e(ly + 10)], fill=LINEA, width=ESCALA)
        lx += 18
    else:
        d.line([e(lx), e(ly + 6), e(lx + 14), e(ly + 6)], fill=LINEA, width=ESCALA)
        for k in (-1, 1):
            d.line([e(lx + 6), e(ly + 6), e(lx + 14), e(ly + 6 + k * 4)],
                   fill=LINEA, width=ESCALA)
        lx += 18
    d.text((e(lx), e(ly + 1)), texto, font=f_leyenda, fill=GRIS)
    lx += ancho_de(texto, f_leyenda) + 18

img = img.resize((ANCHO, ALTO), Image.LANCZOS)
img.save(SALIDA)
print(f"{SALIDA}  {ANCHO}x{ALTO}  ({len(TABLAS)} tablas, {len(RELACIONES)} relaciones)")
for t, cs in TABLAS.items():
    print(f"   {t:<20} {len(cs):>2} columnas")
