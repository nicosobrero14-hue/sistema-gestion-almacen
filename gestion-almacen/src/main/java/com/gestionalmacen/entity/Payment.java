package com.gestionalmacen.entity;

import java.math.BigDecimal;
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

// Pago de una venta (CU-09). Es una tabla aparte porque un pago digital puede tener mas de un intento.
@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "pagos")
public class Payment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_pago")
	private Long id;

	@JsonIgnore
	@ManyToOne
	@JoinColumn(name = "id_venta", nullable = false)
	private Sale sale;

	@Enumerated(EnumType.STRING)
	@Column(name = "metodo", nullable = false, length = 20)
	private PaymentMethod method;

	@Column(name = "monto", nullable = false, precision = 10, scale = 2)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 20)
	private PaymentStatus status;

	@Column(name = "fecha_hora", nullable = false)
	private LocalDateTime dateTime;

	@Column(name = "fecha_confirmacion")
	private LocalDateTime confirmedAt;

	@PrePersist
	private void onCreate() {
		dateTime = LocalDateTime.now();
	}
}
