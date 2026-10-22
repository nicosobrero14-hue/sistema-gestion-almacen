package com.gestionalmacen.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

// Factura de un proveedor (RF-15 / CU-23). Es el egreso del comercio: hasta esta fase el
// sistema registraba solo la plata que entra y no podia decir cuanto se gano.
@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "compras")
public class Purchase {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_compra")
	private Long id;

	// Una factura siempre es de alguien, asi que no puede quedar sin proveedor.
	@ManyToOne
	@JoinColumn(name = "id_proveedor", nullable = false)
	private Supplier supplier;

	// El CUIT que trae el QR del comprobante. Con el se reconoce al proveedor.
	@Column(name = "cuit_emisor", length = 13)
	private String issuerCuit;

	// Codigo de ARCA: 1 factura A, 6 factura B, 11 factura C. Se guarda el numero y no la
	// letra porque es lo que viene en el QR y no deja lugar a dudas.
	@Column(name = "tipo_comprobante", nullable = false)
	private Integer voucherType;

	@Column(name = "punto_venta", nullable = false)
	private Integer salesPoint;

	@Column(name = "numero", nullable = false)
	private Long number;

	// La del comprobante, que no es la del dia en que se carga.
	@Column(name = "fecha", nullable = false)
	private LocalDate date;

	// Lo que dice el papel, que es tambien lo que trae el QR.
	@Column(name = "total_facturado", nullable = false, precision = 12, scale = 2)
	private BigDecimal invoicedTotal;

	// Lo que realmente se debe. Casi siempre es igual al facturado; cambia cuando no se paga
	// el total, por ejemplo si falto un producto del pedido. El saldo se calcula sobre este.
	@Column(name = "total_acordado", nullable = false, precision = 12, scale = 2)
	private BigDecimal agreedTotal;

	@Column(name = "motivo_ajuste", length = 255)
	private String adjustmentReason;

	// Codigo de autorizacion del comprobante.
	@Column(name = "cae", length = 20)
	private String authorizationCode;

	// Nombre del archivo de la foto, no la foto, igual que en los productos.
	@Column(name = "imagen", length = 255)
	private String image;

	@Column(name = "fecha_alta", nullable = false)
	private LocalDateTime createdAt;

	// LAZY: el usuario no sale en el JSON, alcanza con la copia del username.
	@JsonIgnore
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_usuario")
	private User user;

	@Column(name = "username", nullable = false, length = 50)
	private String username;

	// EAGER: una factura casi siempre se mira junto con lo que se le pago.
	@OrderBy("fecha, id")
	@OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private Set<SupplierPayment> payments = new LinkedHashSet<>();

	@PrePersist
	private void onCreate() {
		createdAt = LocalDateTime.now();
	}

	// Lo pagado hasta ahora, sin contar los pagos anulados. No es una columna: un total
	// guardado queda desactualizado apenas se registra un pago.
	public BigDecimal getPaid() {
		BigDecimal pagado = BigDecimal.ZERO;
		for (SupplierPayment payment : payments) {
			if (!payment.isCancelled()) {
				pagado = pagado.add(payment.getAmount());
			}
		}
		return pagado;
	}

	// Lo que falta pagar.
	public BigDecimal getBalance() {
		return agreedTotal.subtract(this.getPaid());
	}

	// El estado sale de la cuenta anterior, por lo mismo que no se guardan las alertas de stock.
	public PurchaseStatus getStatus() {
		BigDecimal pagado = this.getPaid();
		if (pagado.compareTo(BigDecimal.ZERO) == 0) {
			return PurchaseStatus.PENDIENTE;
		}
		return pagado.compareTo(agreedTotal) >= 0 ? PurchaseStatus.PAGADA : PurchaseStatus.PARCIAL;
	}

	// Numero completo del comprobante, como figura impreso: 0003-00004046.
	public String getVoucherNumber() {
		return String.format("%04d-%08d", salesPoint, number);
	}
}
