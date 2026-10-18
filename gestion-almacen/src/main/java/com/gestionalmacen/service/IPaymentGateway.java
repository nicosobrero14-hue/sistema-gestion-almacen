package com.gestionalmacen.service;

import com.gestionalmacen.entity.PaymentStatus;
import com.gestionalmacen.entity.Sale;

// Cobro digital contra un servicio externo (RF-05 / CU-10).
// Esta detras de una interfaz para que las pruebas usen una pasarela falsa y no salgan a internet.
public interface IPaymentGateway {

	// Genera el cobro y devuelve el identificador con el que se lo sigue despues.
	String createOrder(Sale sale);

	// Como viene ese cobro: PENDIENTE si todavia no pagaron, APROBADO si el dinero entro,
	// CANCELADO si vencio el plazo o se dio de baja.
	PaymentStatus getOrderStatus(String orderId);

	// Da de baja el cobro para que el cliente ya no pueda pagarlo.
	void cancelOrder(String orderId);
}
