package com.gestionalmacen.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// Un producto del carrito: cual y cuantos (CU-08). El precio no viaja: lo pone el servidor.
@Getter @Setter
public class SaleItemDTO {

	@NotNull(message = "Falta el producto")
	private Long productId;

	// CU-08 exc. 3a: la cantidad tiene que ser mayor a cero.
	@NotNull(message = "La cantidad es obligatoria")
	@Min(value = 1, message = "La cantidad tiene que ser mayor a cero")
	private Integer quantity;
}
