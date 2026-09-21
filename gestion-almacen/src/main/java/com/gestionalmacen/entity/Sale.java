package com.gestionalmacen.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Venta (RF-03 / CU-11). El id es tambien el numero de ticket.
@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "ventas")
public class Sale {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_venta")
	private Long id;

	@Column(name = "fecha_hora", nullable = false)
	private LocalDateTime dateTime;

	@JsonIgnore
	@ManyToOne
	@JoinColumn(name = "id_usuario")
	private User user;

	// Copia del usuario que hizo la venta (CU-13 paso 3).
	@Column(name = "username", nullable = false, length = 50)
	private String username;

	// Suma de los renglones.
	@Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
	private BigDecimal subtotal;

	@Column(name = "descuento", nullable = false, precision = 10, scale = 2)
	private BigDecimal discount;

	// Subtotal menos descuento.
	@Column(name = "total", nullable = false, precision = 10, scale = 2)
	private BigDecimal total;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 20)
	private SaleStatus status;

	// cascade: los renglones y el pago se guardan junto con la venta.
	// EAGER: la venta siempre se muestra con sus renglones y su pago, que es lo que lleva el ticket.
	// Set y no List: Hibernate no puede traer dos List en la misma consulta.
	@OrderBy("id")
	@OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private Set<SaleDetail> details = new LinkedHashSet<>();

	@OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private Set<Payment> payments = new LinkedHashSet<>();

	@PrePersist
	private void onCreate() {
		dateTime = LocalDateTime.now();
	}
}
