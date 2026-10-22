package com.gestionalmacen.service;

import org.springframework.web.multipart.MultipartFile;

// Guardado de imagenes del sistema: la foto de un producto (RF-14) y la de una factura de
// proveedor (RF-15). El archivo vive en una carpeta del servidor y en la base queda su nombre.
//
// baseName es el nombre sin extension, y siempre lo arma quien llama a partir de un id:
// "producto-7" o "compra-3". Nunca sale de lo que manda el cliente.
public interface IImageStorage {

	// Guarda la imagen que subio el usuario y devuelve el nombre del archivo con su extension.
	String save(String baseName, MultipartFile file);

	// Descarga la foto que sugirio el catalogo publico al escanear un codigo (CU-03 paso 6).
	// Devuelve null si no hay enlace o si no se pudo traer: quedarse sin la foto no impide el alta.
	String downloadFromCatalog(String baseName, String url);

	// Borra la imagen. No falla si no habia ninguna.
	void delete(String baseName);
}
