package com.gestionalmacen.exception;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.gestionalmacen.dto.ErrorDTO;

// Convierte los errores de toda la aplicacion en respuestas JSON con el codigo HTTP que corresponde.
// Asi los controladores no necesitan try/catch.
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	//404- lo que se pidio no existe
	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<ErrorDTO> notFound(NotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDTO(ex.getMessage()));
	}

	//409- los datos estan bien pero rompen una regla del negocio
	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<ErrorDTO> businessRule(BusinessRuleException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorDTO(ex.getMessage()));
	}

	//400- fallo @Valid: devuelve el error de cada campo para marcarlo en rojo en el formulario
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorDTO> validation(MethodArgumentNotValidException ex) {
		Map<String, String> errors = new HashMap<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			errors.putIfAbsent(error.getField(), error.getDefaultMessage());
		}
		return ResponseEntity.badRequest().body(new ErrorDTO("Hay campos con errores", errors));
	}

	//400- el JSON no se puede leer (un precio escrito como texto, un rol que no existe)
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorDTO> unreadableBody(HttpMessageNotReadableException ex) {
		return ResponseEntity.badRequest()
				.body(new ErrorDTO("El formato de los datos no es válido. Revise el tipo de cada campo."));
	}

	//409- choca con una restriccion de la base (por ejemplo, dos altas a la vez con el mismo codigo)
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorDTO> dataIntegrity(DataIntegrityViolationException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(new ErrorDTO("La operación no se pudo completar porque viola una restricción de la base de datos."));
	}

	//500- cualquier otro error. El detalle queda en el log y no se le muestra al usuario
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorDTO> unexpected(Exception ex) {
		log.error("Error inesperado", ex);
		return ResponseEntity.internalServerError()
				.body(new ErrorDTO("Ocurrió un error inesperado. Intente nuevamente."));
	}
}
