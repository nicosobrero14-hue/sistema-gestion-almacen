package com.gestionalmacen.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// Datos que llegan al crear o modificar un producto (CU-02, CU-20).
// Se usa un DTO y no la entidad para que el cliente no pueda mandar el id, activo ni la fecha de alta.
@Getter @Setter
public class ProductDTO {

	@NotBlank(message = "El nombre es obligatorio")
	@Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
	private String name;

	@Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
	private String description;

	// @NotNull aparte, porque @DecimalMin deja pasar un precio vacio.
	@NotNull(message = "El precio es obligatorio")
	@DecimalMin(value = "0.01", message = "El precio debe ser mayor a cero")
	@Digits(integer = 8, fraction = 2, message = "El precio admite hasta 8 enteros y 2 decimales")
	private BigDecimal price;

	// Solo se usa en el alta. Despues el stock se ajusta desde la pantalla de stock.
	@NotNull(message = "El stock es obligatorio")
	@Min(value = 0, message = "El stock no puede ser negativo")
	private Integer stock;

	@NotNull(message = "El stock mínimo es obligatorio")
	@Min(value = 0, message = "El stock mínimo no puede ser negativo")
	private Integer minimumStock;

	@Size(max = 64, message = "El código de barras no puede superar los 64 caracteres")
	private String barcode;

	private LocalDate expirationDate;

	private boolean onOffer;

	@DecimalMin(value = "0.01", message = "El precio de oferta debe ser mayor a cero")
	@Digits(integer = 8, fraction = 2, message = "El precio de oferta admite hasta 8 enteros y 2 decimales")
	private BigDecimal offerPrice;

	// Solo el id: el servicio busca el proveedor.
	private Long supplierId;
}
