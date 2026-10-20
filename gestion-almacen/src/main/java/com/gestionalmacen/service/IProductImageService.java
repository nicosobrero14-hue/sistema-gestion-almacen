package com.gestionalmacen.service;

import org.springframework.web.multipart.MultipartFile;

// Imagen de un producto (RF-12 / CU-21). El archivo vive en una carpeta del servidor y en la base
// queda solo su nombre.
public interface IProductImageService {

	// Guarda la imagen que subio el empleado y devuelve el nombre del archivo.
	String save(Long productId, MultipartFile file);

	// Descarga la imagen que sugirio el catalogo publico al escanear el codigo (CU-03 paso 6).
	// Devuelve null si no hay enlace o si no se pudo traer: quedarse sin la foto no impide el alta.
	String download(Long productId, String url);

	// Borra la imagen del producto. No falla si no tenia ninguna.
	void delete(Long productId);
}
