package com.gestionalmacen.service;

import org.springframework.web.multipart.MultipartFile;

// Imagen de un producto (RF-12 / CU-21). El archivo vive en una carpeta del servidor y en la base
// queda solo su nombre.
public interface IProductImageService {

	// Guarda la imagen que subio el empleado y devuelve el nombre del archivo.
	String save(Long productId, MultipartFile file);

	// Borra la imagen del producto. No falla si no tenia ninguna.
	void delete(Long productId);
}
