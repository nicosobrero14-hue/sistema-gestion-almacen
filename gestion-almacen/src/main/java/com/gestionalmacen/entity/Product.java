package com.gestionalmacen.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

// Producto del catalogo (RF-01).
@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "productos")
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_producto")
	private Long id;

	@Column(name = "nombre", nullable = false, length = 120)
	private String name;

	@Column(name = "descripcion", length = 255)
	private String description;

	// BigDecimal y no double: con dinero no puede haber errores de redondeo.
	@Column(name = "precio", nullable = false, precision = 10, scale = 2)
	private BigDecimal price;

	// DECIMAL y no entero: lo que se vende por peso tiene stock fraccionado (RF-12).
	@Column(name = "stock_actual", nullable = false, precision = 10, scale = 3)
	private BigDecimal stock;

	// Umbral de la alerta de stock bajo (RF-09).
	@Column(name = "stock_minimo", nullable = false, precision = 10, scale = 3)
	private BigDecimal minimumStock;

	// Define si "precio" es por unidad o por kilo, y como se carga el producto en una venta (RF-12).
	@Enumerated(EnumType.STRING)
	@Column(name = "unidad_venta", nullable = false, length = 10)
	private SaleUnit saleUnit = SaleUnit.UNIDAD;

	@Column(name = "codigo_barras", length = 64, unique = true)
	private String barcode;

	// Solo para productos perecederos.
	@Column(name = "fecha_vencimiento")
	private LocalDate expirationDate;

	@Column(name = "en_oferta", nullable = false)
	private boolean onOffer = false;

	@Column(name = "precio_oferta", precision = 10, scale = 2)
	private BigDecimal offerPrice;

	// Baja logica: en false deja de aparecer, pero no se borra.
	@Column(name = "activo", nullable = false)
	private boolean active = true;

	@Column(name = "fecha_alta", nullable = false)
	private LocalDateTime createdAt;

	// Al traer un producto se trae tambien su proveedor. Puede no tener.
	@ManyToOne
	@JoinColumn(name = "id_proveedor")
	private Supplier supplier;

	@PrePersist
	private void onCreate() {
		createdAt = LocalDateTime.now();
	}

	// No es una columna, se calcula. En el JSON sale como "lowStock".
	public boolean isLowStock() {
		return stock.compareTo(minimumStock) <= 0;
	}

	// El precio que se cobra: el de oferta si esta en oferta. En el JSON sale como "salePrice".
	public BigDecimal getSalePrice() {
		return onOffer ? offerPrice : price;
	}
}
