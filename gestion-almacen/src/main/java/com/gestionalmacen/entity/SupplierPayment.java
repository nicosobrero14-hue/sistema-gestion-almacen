package com.gestionalmacen.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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

// Pago a un proveedor (RF-15 / CU-24). Va aparte de la factura porque no coinciden:
// se le puede pagar en dos veces, o pagarle tres facturas el mismo dia.
@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "pagos_proveedor")
public class SupplierPayment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_pago")
	private Long id;

	// LAZY: la factura no sale en el JSON del pago, asi que no se busca en la base.
	@JsonIgnore
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_compra", nullable = false)
	private Purchase purchase;

	// La fecha en que se pago, que puede no ser la de hoy.
	@Column(name = "fecha", nullable = false)
	private LocalDate date;

	@Column(name = "monto", nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	// Se reusa el mismo enum que las ventas: son las mismas formas de pago.
	@Enumerated(EnumType.STRING)
	@Column(name = "metodo", nullable = false, length = 20)
	private PaymentMethod method;

	// Un pago no se borra, se anula, igual que una venta no se borra sino que se cancela.
	// Asi queda el rastro de que estuvo registrado.
	@Column(name = "anulado", nullable = false)
	private boolean cancelled = false;

	@Column(name = "fecha_alta", nullable = false)
	private LocalDateTime createdAt;

	@JsonIgnore
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_usuario")
	private User user;

	@Column(name = "username", nullable = false, length = 50)
	private String username;

	@PrePersist
	private void onCreate() {
		createdAt = LocalDateTime.now();
	}
}
