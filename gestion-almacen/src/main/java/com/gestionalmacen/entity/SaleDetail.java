package com.gestionalmacen.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Renglon de una venta: un producto, su precio y la cantidad (CU-08).
@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "detalle_venta")
public class SaleDetail {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_detalle")
	private Long id;

	// LAZY en las dos relaciones: no salen en el JSON, asi que no se buscan en la base salvo que el codigo las use.
	// @JsonIgnore: la venta ya incluye sus renglones; si el renglon incluyera la venta, el JSON no terminaria nunca.
	@JsonIgnore
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_venta", nullable = false)
	private Sale sale;

	@JsonIgnore
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_producto")
	private Product product;

	// Copias del nombre y del precio: el ticket no cambia aunque despues cambie el producto.
	@Column(name = "nombre_producto", nullable = false, length = 120)
	private String productName;

	@Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
	private BigDecimal unitPrice;

	// Copia de como se vendia el producto, igual que el nombre y el precio: el ticket no
	// cambia aunque despues el producto se reclasifique (RF-12).
	@Enumerated(EnumType.STRING)
	@Column(name = "unidad_venta", nullable = false, length = 10)
	private SaleUnit saleUnit;

	// Fraccionada en los productos que se venden por peso: 0,120 son 120 gramos (RF-12).
	@Column(name = "cantidad", nullable = false, precision = 10, scale = 3)
	private BigDecimal quantity;

	// Arma el renglon con el nombre y el precio que tiene el producto en este momento.
	public SaleDetail(Sale sale, Product product, BigDecimal quantity) {
		this.sale = sale;
		this.product = product;
		this.productName = product.getName();
		this.unitPrice = product.getSalePrice();
		this.saleUnit = product.getSaleUnit();
		this.quantity = quantity;
	}

	// No es una columna, se calcula. En el JSON sale como "subtotal".
	// Se redondea a dos decimales: por peso el producto casi nunca da un importe exacto,
	// y la regla de redondeo tiene que ser siempre la misma o la caja no cierra.
	public BigDecimal getSubtotal() {
		return unitPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
	}
}
