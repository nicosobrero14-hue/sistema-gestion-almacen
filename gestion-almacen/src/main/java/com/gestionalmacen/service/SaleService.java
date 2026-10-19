package com.gestionalmacen.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestionalmacen.dto.SaleDTO;
import com.gestionalmacen.dto.SaleItemDTO;
import com.gestionalmacen.entity.MovementType;
import com.gestionalmacen.entity.Payment;
import com.gestionalmacen.entity.PaymentMethod;
import com.gestionalmacen.entity.PaymentStatus;
import com.gestionalmacen.entity.Product;
import com.gestionalmacen.entity.Sale;
import com.gestionalmacen.entity.SaleDetail;
import com.gestionalmacen.entity.SaleStatus;
import com.gestionalmacen.entity.StockMovement;
import com.gestionalmacen.entity.User;
import com.gestionalmacen.exception.BusinessRuleException;
import com.gestionalmacen.exception.NotFoundException;
import com.gestionalmacen.repository.IProductRepository;
import com.gestionalmacen.repository.ISaleRepository;
import com.gestionalmacen.repository.IStockMovementRepository;

@Service
public class SaleService implements ISaleService {

	@Autowired
	private ISaleRepository saleRepository;

	@Autowired
	private IProductRepository productRepository;

	@Autowired
	private IStockMovementRepository movementRepository;

	// Para buscar cada producto, con el mismo 404 que el resto de la aplicacion.
	@Autowired
	private IProductService productService;

	// Para saber quien hace la venta (CU-13 paso 3).
	@Autowired
	private IUserService userService;

	// El cobro digital. En las pruebas es una pasarela falsa (CU-10).
	@Autowired
	private IPaymentGateway paymentGateway;

	// @Transactional: la venta, el pago, el stock y los movimientos se guardan juntos.
	// Si algo falla, no se guarda nada y el stock queda como estaba.
	@Override
	@Transactional
	public Sale saveSale(SaleDTO saleDTO) {
		User user = userService.getSessionUser();

		Sale sale = this.buildSale(saleDTO, user);
		// Guarda la venta con sus renglones y su pago. Desde aca la venta ya tiene su numero.
		saleRepository.save(sale);

		// Con MercadoPago el cobro lo hace el cliente desde su telefono, asi que la venta queda
		// esperando y el stock no se toca hasta que el dinero entre (CU-10, CU-11 exc. 4a).
		if (saleDTO.getPaymentMethod() == PaymentMethod.MERCADOPAGO) {
			String orderId = paymentGateway.createOrder(sale);
			sale.getPayments().forEach(payment -> payment.setExternalReference(orderId));
			return sale;
		}

		// En efectivo y por transferencia el cobro ya esta hecho, asi que se confirma en el acto (CU-09).
		this.confirmSale(sale, user);
		return sale;
	}

	// CU-10 pasos 5 y 6: se le pregunta a MercadoPago como viene el cobro.
	// La pantalla llama a esto cada pocos segundos mientras el cliente paga.
	@Override
	@Transactional
	public Sale checkPayment(Long id) {
		Sale sale = this.findSale(id);
		// Si ya se resolvio, no hay nada que preguntar: la respuesta anterior ya la definio.
		if (sale.getStatus() != SaleStatus.PENDIENTE_PAGO) {
			return sale;
		}

		PaymentStatus estado = paymentGateway.getOrderStatus(this.paymentOf(sale).getExternalReference());
		if (estado == PaymentStatus.APROBADO) {
			// El dinero entro: recien aca se descuenta el stock. La venta la hizo el empleado que la registro.
			this.confirmSale(sale, sale.getUser());
		} else if (estado == PaymentStatus.CANCELADO) {
			this.cancelSale(sale);
		}
		return sale;
	}

	// CU-10 exc. 5a: el cliente no paga y el cajero da de baja el cobro.
	@Override
	@Transactional
	public Sale cancelPayment(Long id) {
		Sale sale = this.findSale(id);
		if (sale.getStatus() != SaleStatus.PENDIENTE_PAGO) {
			throw new BusinessRuleException("La venta N° " + id + " ya no está esperando un pago.");
		}

		paymentGateway.cancelOrder(this.paymentOf(sale).getExternalReference());
		this.cancelSale(sale);
		return sale;
	}

	// El cobro no se completo: la venta queda cancelada y el stock nunca se toco.
	private void cancelSale(Sale sale) {
		sale.setStatus(SaleStatus.CANCELADA);
		for (Payment payment : sale.getPayments()) {
			payment.setStatus(PaymentStatus.CANCELADO);
		}
	}

	// Por ahora una venta tiene un solo pago. La tabla admite varios por si mas adelante se cobra en partes.
	private Payment paymentOf(Sale sale) {
		return sale.getPayments().stream().findFirst()
				.orElseThrow(() -> new BusinessRuleException("La venta N° " + sale.getId() + " no tiene un pago registrado."));
	}

	// Arma la venta en memoria: los renglones, los totales y el pago. Todavia no toca el stock.
	private Sale buildSale(SaleDTO saleDTO, User user) {
		Sale sale = new Sale();
		sale.setUser(user);
		sale.setUsername(user.getUsername());
		sale.setStatus(SaleStatus.PENDIENTE_PAGO);

		//1- arma los renglones con el precio de hoy y controla el stock de cada producto
		BigDecimal subtotal = BigDecimal.ZERO;
		Set<Long> productIds = new HashSet<>();
		for (SaleItemDTO item : saleDTO.getItems()) {
			Product product = this.findProductToSell(item.getProductId(), item.getQuantity());

			// El carrito junta las unidades de un producto en un solo renglon. Repetido, podria vender mas de lo que hay.
			if (!productIds.add(product.getId())) {
				throw new BusinessRuleException("El producto " + product.getName() + " está repetido en la venta.");
			}

			SaleDetail detail = new SaleDetail(sale, product, item.getQuantity());
			sale.getDetails().add(detail);
			subtotal = subtotal.add(detail.getSubtotal());
		}

		//2- descuento y total (RF-03)
		BigDecimal discount = saleDTO.getDiscount() == null ? BigDecimal.ZERO : saleDTO.getDiscount();
		// El total tiene que quedar mayor a cero: un pago de cero pesos no es un pago.
		if (discount.compareTo(subtotal) >= 0) {
			throw new BusinessRuleException("El descuento tiene que ser menor al subtotal.");
		}
		sale.setSubtotal(subtotal);
		sale.setDiscount(discount);
		sale.setTotal(subtotal.subtract(discount));

		//3- el pago queda pendiente hasta que se confirme el cobro (CU-09)
		Payment payment = new Payment();
		payment.setSale(sale);
		payment.setMethod(saleDTO.getPaymentMethod());
		payment.setAmount(sale.getTotal());
		payment.setStatus(PaymentStatus.PENDIENTE);
		sale.getPayments().add(payment);
		return sale;
	}

	// Da el cobro por hecho: aprueba el pago, descuenta el stock y deja un movimiento por producto (CU-12).
	// Lo llama la venta en efectivo o por transferencia apenas se registra.
	private void confirmSale(Sale sale, User user) {
		sale.setStatus(SaleStatus.CONFIRMADA);
		for (Payment payment : sale.getPayments()) {
			payment.setStatus(PaymentStatus.APROBADO);
			payment.setConfirmedAt(LocalDateTime.now());
		}

		for (SaleDetail detail : sale.getDetails()) {
			Product product = detail.getProduct();
			BigDecimal previousStock = product.getStock();
			// Con un pago digital pasa tiempo entre que se genera el cobro y que entra el dinero. Hoy el
			// comercio tiene una sola caja, asi que nadie puede vender en el medio, pero si el stock no
			// alcanzara no se puede dejar negativo: la base lo rechaza y el cobro ya se hizo.
			BigDecimal missing = detail.getQuantity().subtract(previousStock).max(BigDecimal.ZERO);
			product.setStock(previousStock.subtract(detail.getQuantity()).add(missing));
			productRepository.save(product);

			String reason = "Venta N° " + sale.getId()
					+ (missing.signum() > 0 ? " (faltaban " + missing.stripTrailingZeros().toPlainString()
							+ " al cobrar)" : "");
			StockMovement movement = new StockMovement(product, user, MovementType.VENTA, previousStock, reason);
			movement.setSale(sale);
			movementRepository.save(movement);
		}
	}

	@Override
	public Sale findSale(Long id) {
		return saleRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("No existe la venta N° " + id));
	}

	@Override
	public List<Sale> getSales(LocalDate from, LocalDate to, String username, String product, String reference) {
		if (from.isAfter(to)) {
			throw new BusinessRuleException("La fecha desde no puede ser posterior a la fecha hasta.");
		}
		// "Hasta" incluye todo ese dia: se busca hasta el comienzo del dia siguiente.
		return saleRepository.search(from.atStartOfDay(), to.plusDays(1).atStartOfDay(), username.trim(),
				product.trim(), reference.trim());
	}

	// El producto tiene que estar activo y tener stock suficiente (CU-07 exc. 5a, CU-08 exc. 4a).
	private Product findProductToSell(Long productId, BigDecimal quantity) {
		Product product = productService.findProduct(productId);

		if (!product.isActive()) {
			throw new BusinessRuleException("El producto " + product.getName() + " está dado de baja.");
		}
		if (quantity.compareTo(product.getStock()) > 0) {
			throw new BusinessRuleException("No hay stock suficiente de " + product.getName()
					+ ". Disponible: " + product.getStock() + ".");
		}
		return product;
	}
}
