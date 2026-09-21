package com.gestionalmacen.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Cada cambio de stock con su motivo, quien lo hizo y cuando (RF-02 / CU-04).
@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "movimientos_stock")
public class StockMovement {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_movimiento")
	private Long id;

	// @JsonIgnore: en el JSON alcanza con el nombre, no hace falta el producto entero.
	@JsonIgnore
	@ManyToOne
	@JoinColumn(name = "id_producto")
	private Product product;

	// Copia del nombre: el movimiento se puede leer aunque el producto cambie de nombre.
	@Column(name = "nombre_producto", nullable = false, length = 120)
	private String productName;

	@JsonIgnore
	@ManyToOne
	@JoinColumn(name = "id_usuario")
	private User user;

	// Copia del usuario, por el mismo motivo.
	@Column(name = "username", nullable = false, length = 50)
	private String username;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", nullable = false, length = 20)
	private MovementType type;

	@Column(name = "stock_anterior", nullable = false)
	private int previousStock;

	@Column(name = "stock_nuevo", nullable = false)
	private int newStock;

	@Column(name = "motivo", nullable = false, length = 255)
	private String reason;

	@Column(name = "fecha_hora", nullable = false)
	private LocalDateTime dateTime;

	// Arma el movimiento a partir del producto ya actualizado: su stock de ahora es el stock nuevo.
	public StockMovement(Product product, User user, MovementType type, int previousStock, String reason) {
		this.product = product;
		this.productName = product.getName();
		this.user = user;
		this.username = user.getUsername();
		this.type = type;
		this.previousStock = previousStock;
		this.newStock = product.getStock();
		this.reason = reason;
	}

	@PrePersist
	private void onCreate() {
		dateTime = LocalDateTime.now();
	}
}
