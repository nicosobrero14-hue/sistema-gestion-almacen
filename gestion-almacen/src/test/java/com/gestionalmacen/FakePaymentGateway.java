package com.gestionalmacen;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.gestionalmacen.entity.PaymentStatus;
import com.gestionalmacen.entity.Sale;
import com.gestionalmacen.service.IPaymentGateway;

// Pasarela de prueba: reemplaza a MercadoPago para que las pruebas no salgan a internet
// ni necesiten credenciales. Guarda las ordenes en memoria y deja simular que el cliente
// pago o que el cobro vencio.
@Service
@Profile("test")
public class FakePaymentGateway implements IPaymentGateway {

	private final Map<String, PaymentStatus> orders = new HashMap<>();
	private int counter = 0;

	@Override
	public String createOrder(Sale sale) {
		String orderId = "ORD-PRUEBA-" + (++counter);
		orders.put(orderId, PaymentStatus.PENDIENTE);
		return orderId;
	}

	@Override
	public PaymentStatus getOrderStatus(String orderId) {
		return orders.getOrDefault(orderId, PaymentStatus.PENDIENTE);
	}

	@Override
	public void cancelOrder(String orderId) {
		orders.put(orderId, PaymentStatus.CANCELADO);
	}

	// ---------- para las pruebas ----------

	// Simula que el cliente escaneo el QR y pago.
	public void approve(String orderId) {
		orders.put(orderId, PaymentStatus.APROBADO);
	}

	// Simula que paso el plazo sin que nadie pagara.
	public void expire(String orderId) {
		orders.put(orderId, PaymentStatus.CANCELADO);
	}

	// El bean es unico para toda la ejecucion: cada prueba arranca sin ordenes de la anterior.
	public void reset() {
		orders.clear();
		counter = 0;
	}
}
