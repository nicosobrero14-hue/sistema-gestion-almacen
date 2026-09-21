package com.gestionalmacen.exception;

// Se pidio algo que no existe. El manejador global la convierte en 404.
public class NotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public NotFoundException(String message) {
		super(message);
	}
}
