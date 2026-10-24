package com.gestionalmacen.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.gestionalmacen.entity.SaleUnit;

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

	// Lo que le cuesta al comercio. Es opcional: un producto puede estar cargado antes de que
	// llegue su primera factura. Sin costo, ese producto no entra en el calculo de la ganancia
	// y el panel principal avisa cuantos renglones quedaron afuera (RF-15).
	@DecimalMin(value = "0", message = "El precio de costo no puede ser negativo")
	@Digits(integer = 8, fraction = 2, message = "El precio de costo admite hasta 8 enteros y 2 decimales")
	private BigDecimal costPrice;

	// Solo se usa en el alta. Despues el stock se ajusta desde la pantalla de stock.
	// Decimal: lo que se vende por peso tiene stock fraccionado (RF-12).
	@NotNull(message = "El stock es obligatorio")
	@DecimalMin(value = "0", message = "El stock no puede ser negativo")
	@Digits(integer = 7, fraction = 3, message = "El stock admite hasta 3 decimales")
	private BigDecimal stock;

	@NotNull(message = "El stock mínimo es obligatorio")
	@DecimalMin(value = "0", message = "El stock mínimo no puede ser negativo")
	@Digits(integer = 7, fraction = 3, message = "El stock mínimo admite hasta 3 decimales")
	private BigDecimal minimumStock;

	// Como se vende: por unidad o por kilo. Si no viene, por unidad.
	private SaleUnit saleUnit = SaleUnit.UNIDAD;

	@Size(max = 64, message = "El código de barras no puede superar los 64 caracteres")
	private String barcode;

	private LocalDate expirationDate;

	// El enlace a la foto que sugirio el catalogo publico, al escanear el codigo en el alta
	// (CU-03 paso 6) o al pedirla desde la ficha de un producto ya cargado (CU-22).
	// El sistema la descarga y guarda el archivo; el enlace no se guarda en ningun lado.
	@Size(max = 500, message = "El enlace de la imagen es demasiado largo")
	private String imageUrl;

	private boolean onOffer;

	@DecimalMin(value = "0.01", message = "El precio de oferta debe ser mayor a cero")
	@Digits(integer = 8, fraction = 2, message = "El precio de oferta admite hasta 8 enteros y 2 decimales")
	private BigDecimal offerPrice;

	// Solo el id: el servicio busca el proveedor.
	private Long supplierId;
}
