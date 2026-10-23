package com.gestionalmacen.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.gestionalmacen.dto.PurchaseDTO;
import com.gestionalmacen.dto.SupplierPaymentDTO;
import com.gestionalmacen.entity.Purchase;
import com.gestionalmacen.entity.Supplier;
import com.gestionalmacen.entity.SupplierPayment;
import com.gestionalmacen.entity.User;
import com.gestionalmacen.exception.BusinessRuleException;
import com.gestionalmacen.exception.NotFoundException;
import com.gestionalmacen.repository.IPurchaseRepository;

@Service
public class PurchaseService implements IPurchaseService {

	@Autowired
	private IPurchaseRepository purchaseRepository;

	@Autowired
	private ISupplierService supplierService;

	@Autowired
	private IUserService userService;

	// Guarda la foto del comprobante (CU-23).
	@Autowired
	private IImageStorage imageStorage;

	// El nombre del archivo sale del id de la compra, nunca de lo que manda el cliente.
	private String imageName(Long purchaseId) {
		return "compra-" + purchaseId;
	}

	@Override
	public List<Purchase> getPurchases(LocalDate from, LocalDate to, Long supplierId) {
		return purchaseRepository.search(from, to, supplierId);
	}

	@Override
	public Purchase findPurchase(Long id) {
		return purchaseRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("No existe la compra con id " + id));
	}

	@Override
	public Purchase savePurchase(PurchaseDTO purchaseDTO) {
		Supplier supplier = supplierService.findSupplier(purchaseDTO.getSupplierId());
		if (!supplier.isActive()) {
			throw new BusinessRuleException("El proveedor " + supplier.getName() + " está dado de baja.");
		}

		// El mismo comprobante no se puede cargar dos veces. La base tambien lo impide, pero
		// conviene avisarlo con un mensaje que se entienda antes de que salte la restriccion.
		if (purchaseRepository.existsBySupplierIdAndVoucherTypeAndSalesPointAndNumber(supplier.getId(),
				purchaseDTO.getVoucherType(), purchaseDTO.getSalesPoint(), purchaseDTO.getNumber())) {
			throw new BusinessRuleException("El comprobante "
					+ String.format("%04d-%08d", purchaseDTO.getSalesPoint(), purchaseDTO.getNumber())
					+ " de " + supplier.getName() + " ya está cargado.");
		}

		Purchase purchase = new Purchase();
		purchase.setSupplier(supplier);
		purchase.setIssuerCuit(purchaseDTO.getIssuerCuit());
		purchase.setVoucherType(purchaseDTO.getVoucherType());
		purchase.setSalesPoint(purchaseDTO.getSalesPoint());
		purchase.setNumber(purchaseDTO.getNumber());
		purchase.setDate(purchaseDTO.getDate());
		purchase.setInvoicedTotal(purchaseDTO.getInvoicedTotal());
		purchase.setAuthorizationCode(purchaseDTO.getAuthorizationCode());
		this.copyAgreedTotal(purchaseDTO, purchase);

		User user = userService.getSessionUser();
		purchase.setUser(user);
		purchase.setUsername(user.getUsername());
		return purchaseRepository.save(purchase);
	}

	// CU-23: el archivo se guarda en el servidor y en la compra queda solo su nombre.
	@Override
	public Purchase saveImage(Long id, MultipartFile file) {
		Purchase purchase = this.findPurchase(id);
		purchase.setImage(imageStorage.save(this.imageName(id), file));
		return purchaseRepository.save(purchase);
	}

	@Override
	public void deleteImage(Long id) {
		Purchase purchase = this.findPurchase(id);
		imageStorage.delete(this.imageName(id));
		purchase.setImage(null);
		purchaseRepository.save(purchase);
	}

	// CU-24: la plata que sale. Cada pago descuenta del saldo de la factura.
	@Override
	public Purchase addPayment(Long id, SupplierPaymentDTO paymentDTO) {
		Purchase purchase = this.findPurchase(id);

		// No se puede pagar mas de lo que se debe: si se pago de mas, lo que esta mal es el total.
		if (paymentDTO.getAmount().compareTo(purchase.getBalance()) > 0) {
			throw new BusinessRuleException("El pago no puede superar el saldo, que es de "
					+ purchase.getBalance() + ".");
		}
		// Un pago anterior a la factura no tiene sentido y suele ser un error de tipeo en la fecha.
		if (paymentDTO.getDate().isBefore(purchase.getDate())) {
			throw new BusinessRuleException("El pago no puede ser anterior a la factura.");
		}

		User user = userService.getSessionUser();
		SupplierPayment payment = new SupplierPayment();
		payment.setPurchase(purchase);
		payment.setDate(paymentDTO.getDate());
		payment.setAmount(paymentDTO.getAmount());
		payment.setMethod(paymentDTO.getMethod());
		payment.setUser(user);
		payment.setUsername(user.getUsername());

		purchase.getPayments().add(payment);
		return purchaseRepository.save(purchase);
	}

	@Override
	public Purchase cancelPayment(Long id, Long paymentId) {
		Purchase purchase = this.findPurchase(id);
		SupplierPayment payment = purchase.getPayments().stream()
				.filter(p -> p.getId().equals(paymentId))
				.findFirst()
				.orElseThrow(() -> new NotFoundException("El pago " + paymentId + " no es de esta factura."));

		if (payment.isCancelled()) {
			throw new BusinessRuleException("Ese pago ya estaba anulado.");
		}
		payment.setCancelled(true);
		return purchaseRepository.save(purchase);
	}

	// El total acordado es lo que realmente se debe. Casi siempre coincide con el facturado y
	// no hay nada que aclarar; cuando no coincide, hay que decir por que.
	private void copyAgreedTotal(PurchaseDTO purchaseDTO, Purchase purchase) {
		BigDecimal facturado = purchaseDTO.getInvoicedTotal();
		BigDecimal acordado = purchaseDTO.getAgreedTotal() == null ? facturado : purchaseDTO.getAgreedTotal();

		if (acordado.compareTo(facturado) > 0) {
			throw new BusinessRuleException("El total acordado no puede ser mayor al facturado.");
		}
		boolean difieren = acordado.compareTo(facturado) != 0;
		if (difieren && (purchaseDTO.getAdjustmentReason() == null
				|| purchaseDTO.getAdjustmentReason().isBlank())) {
			throw new BusinessRuleException("Si el total acordado no es el facturado, hay que indicar el motivo.");
		}

		purchase.setAgreedTotal(acordado);
		purchase.setAdjustmentReason(difieren ? purchaseDTO.getAdjustmentReason() : null);
	}
}
