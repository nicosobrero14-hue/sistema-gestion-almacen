package com.gestionalmacen.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestionalmacen.dto.SaleDTO;
import com.gestionalmacen.dto.SaleItemDTO;
import com.gestionalmacen.entity.MovementType;
import com.gestionalmacen.entity.Payment;
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

	// @Transactional: la venta, el pago, el stock y los movimientos se guardan juntos.
	// Si algo falla, no se guarda nada y el stock queda como estaba.
	@Override
	@Transactional
	public Sale saveSale(SaleDTO saleDTO) {
		User user = userService.getSessionUser();

		Sale sale = new Sale();
		sale.setUser(user);
		sale.setUsername(user.getUsername());
		sale.setStatus(SaleStatus.CONFIRMADA);

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

		//3- el pago: en efectivo o por transferencia se cobra en el momento, asi que queda aprobado (CU-09)
		Payment payment = new Payment();
		payment.setSale(sale);
		payment.setMethod(saleDTO.getPaymentMethod());
		payment.setAmount(sale.getTotal());
		payment.setStatus(PaymentStatus.APROBADO);
		payment.setConfirmedAt(LocalDateTime.now());
		sale.getPayments().add(payment);

		// Guarda la venta con sus renglones y su pago. Desde aca la venta ya tiene su numero.
		saleRepository.save(sale);

		//4- descuenta el stock y deja un movimiento por producto, con el numero de venta (CU-12)
		for (SaleDetail detail : sale.getDetails()) {
			Product product = detail.getProduct();
			int previousStock = product.getStock();
			product.setStock(previousStock - detail.getQuantity());
			productRepository.save(product);

			StockMovement movement = new StockMovement(product, user, MovementType.VENTA, previousStock,
					"Venta N° " + sale.getId());
			movement.setSale(sale);
			movementRepository.save(movement);
		}
		return sale;
	}

	@Override
	public Sale findSale(Long id) {
		return saleRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("No existe la venta N° " + id));
	}

	// El producto tiene que estar activo y tener stock suficiente (CU-07 exc. 5a, CU-08 exc. 4a).
	private Product findProductToSell(Long productId, int quantity) {
		Product product = productService.findProduct(productId);

		if (!product.isActive()) {
			throw new BusinessRuleException("El producto " + product.getName() + " está dado de baja.");
		}
		if (quantity > product.getStock()) {
			throw new BusinessRuleException("No hay stock suficiente de " + product.getName()
					+ ". Disponible: " + product.getStock() + ".");
		}
		return product;
	}
}
