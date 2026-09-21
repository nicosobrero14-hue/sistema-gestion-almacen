// El lector de codigo de barras funciona como un teclado: escribe el codigo y aprieta Enter (CU-03).
// Los codigos de los productos (EAN) son solo numeros: un nombre seguido de Enter no se toma como codigo.
export const isBarcode = (text) => /^\d+$/.test(text.trim())
