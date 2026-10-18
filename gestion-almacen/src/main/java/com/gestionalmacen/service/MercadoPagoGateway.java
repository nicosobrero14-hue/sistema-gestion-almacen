package com.gestionalmacen.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.gestionalmacen.entity.PaymentStatus;
import com.gestionalmacen.entity.Sale;
import com.gestionalmacen.entity.SaleDetail;
import com.gestionalmacen.exception.BusinessRuleException;

// Cobro con el codigo QR de MercadoPago (CU-10). El QR esta impreso y pegado en el mostrador,
// asociado a la caja del comercio: lo que cambia en cada venta es la orden que se carga sobre esa caja.
// El sistema no se comunica con ningun equipo, solo con la API.
// @Profile("!test"): las pruebas usan la pasarela falsa, asi no dependen de internet ni de credenciales.
@Service
@Profile("!test")
public class MercadoPagoGateway implements IPaymentGateway {

	private static final String API = "https://api.mercadopago.com/v1/orders";
	private static final ParameterizedTypeReference<Map<String, Object>> RESPUESTA = new ParameterizedTypeReference<>() {
	};

	// Las credenciales viven en application-local.properties, que no se sube al repositorio (RNF-02).
	@Value("${mercadopago.access-token:}")
	private String accessToken;

	// Identificador de la caja a la que esta asociado el QR impreso.
	@Value("${mercadopago.pos-external-id:}")
	private String posExternalId;

	// Cuanto tiempo tiene el cliente para pagar antes de que el cobro venza (CU-10 exc. 5a).
	@Value("${mercadopago.expiracion:PT5M}")
	private String expiracion;

	@Override
	public String createOrder(Sale sale) {
		Map<String, Object> cuerpo = Map.of(
				"type", "qr",
				"total_amount", sale.getTotal().toPlainString(),
				"description", "Venta N° " + sale.getId(),
				"external_reference", "venta-" + sale.getId(),
				"expiration_time", expiracion,
				"config", Map.of("qr", Map.of("external_pos_id", posExternalId, "mode", "static")),
				"transactions", Map.of("payments", List.of(Map.of("amount", sale.getTotal().toPlainString()))),
				"items", this.items(sale));

		Map<String, Object> respuesta = this.pedir(() -> this.cliente().post()
				.contentType(MediaType.APPLICATION_JSON)
				// La clave sale del numero de venta y no es al azar: si el pedido se reintenta por un corte
				// de red, MercadoPago reconoce que es el mismo y no genera un segundo cobro.
				.header("X-Idempotency-Key", "venta-" + sale.getId())
				.body(cuerpo)
				.retrieve()
				.body(RESPUESTA));

		return String.valueOf(respuesta.get("id"));
	}

	@Override
	public PaymentStatus getOrderStatus(String orderId) {
		Map<String, Object> respuesta = this.pedir(() -> this.cliente().get()
				.uri("/{id}", orderId)
				.retrieve()
				.body(RESPUESTA));

		return this.traducir(String.valueOf(respuesta.get("status")));
	}

	@Override
	public void cancelOrder(String orderId) {
		// MercadoPago solo deja cancelar mientras nadie pago todavia.
		this.pedir(() -> this.cliente().post()
				.uri("/{id}/cancel", orderId)
				.header("X-Idempotency-Key", "cancelar-" + orderId)
				.retrieve()
				.body(RESPUESTA));
	}

	// MercadoPago habla de created, processed, expired y canceled. El sistema usa sus propios estados.
	private PaymentStatus traducir(String estado) {
		return switch (estado) {
			case "processed" -> PaymentStatus.APROBADO;
			case "expired", "canceled" -> PaymentStatus.CANCELADO;
			default -> PaymentStatus.PENDIENTE;
		};
	}

	// El detalle viaja para que el cliente vea en su telefono que esta pagando, y no solo un importe.
	private List<Map<String, Object>> items(Sale sale) {
		List<Map<String, Object>> items = new ArrayList<>();
		for (SaleDetail detail : sale.getDetails()) {
			items.add(Map.of(
					"title", detail.getProductName(),
					"unit_price", detail.getUnitPrice().toPlainString(),
					"quantity", detail.getQuantity()));
		}
		return items;
	}

	private RestClient cliente() {
		if (accessToken.isBlank() || posExternalId.isBlank()) {
			throw new BusinessRuleException(
					"El cobro con MercadoPago no está configurado en este equipo. Cobre en efectivo o por transferencia.");
		}
		return RestClient.builder()
				.baseUrl(API)
				.defaultHeader("Authorization", "Bearer " + accessToken)
				.build();
	}

	// Si MercadoPago no responde o rechaza el pedido, el cajero tiene que ver un mensaje util
	// y no un error 500. El detalle queda en el log del manejador global.
	private Map<String, Object> pedir(java.util.function.Supplier<Map<String, Object>> llamada) {
		try {
			return llamada.get();
		} catch (RestClientException e) {
			throw new BusinessRuleException("No se pudo contactar a MercadoPago. Intente de nuevo o cobre de otra forma.");
		}
	}
}
