package com.gestionalmacen.entity;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

	// @JsonIgnore: la venta ya incluye sus renglones; si el renglon incluyera la venta, el JSON no terminaria nunca.
	@JsonIgnore
	@ManyToOne
	@JoinColumn(name = "id_venta", nullable = false)
	private Sale sale;

	@JsonIgnore
	@ManyToOne
	@JoinColumn(name = "id_producto")
	private Product product;

	// Copias del nombre y del precio: el ticket no cambia aunque despues cambie el producto.
	@Column(name = "nombre_producto", nullable = false, length = 120)
	private String productName;

	@Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
	private BigDecimal unitPrice;

	@Column(name = "cantidad", nullable = false)
	private int quantity;

	// Arma el renglon con el nombre y el precio que tiene el producto en este momento.
	public SaleDetail(Sale sale, Product product, int quantity) {
		this.sale = sale;
		this.product = product;
		this.productName = product.getName();
		this.unitPrice = product.getSalePrice();
		this.quantity = quantity;
	}

	// No es una columna, se calcula. En el JSON sale como "subtotal".
	public BigDecimal getSubtotal() {
		return unitPrice.multiply(BigDecimal.valueOf(quantity));
	}
}
