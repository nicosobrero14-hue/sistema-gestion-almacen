package com.gestionalmacen.exception;

// Los datos estan bien pero rompen una regla del negocio. El manejador global la convierte en 409.
public class BusinessRuleException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public BusinessRuleException(String message) {
		super(message);
	}
}
