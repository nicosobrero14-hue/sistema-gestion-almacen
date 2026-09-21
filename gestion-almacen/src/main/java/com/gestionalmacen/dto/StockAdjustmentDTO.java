package com.gestionalmacen.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// Datos que llegan al ajustar el stock a mano (CU-04).
@Getter @Setter
public class StockAdjustmentDTO {

	// CU-04 paso 5: el stock nuevo, no la diferencia. Es lo que se cuenta en la estanteria.
	@NotNull(message = "El stock nuevo es obligatorio")
	@Min(value = 0, message = "El stock no puede ser negativo")
	private Integer newStock;

	@NotBlank(message = "El motivo es obligatorio")
	@Size(max = 255, message = "El motivo no puede superar los 255 caracteres")
	private String reason;
}
