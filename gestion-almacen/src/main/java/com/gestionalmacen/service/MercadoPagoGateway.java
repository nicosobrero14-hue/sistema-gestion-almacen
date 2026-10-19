package com.gestionalmacen.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.gestionalmacen.entity.PaymentStatus;
import com.gestionalmacen.entity.Sale;
import com.gestionalmacen.entity.SaleDetail;
import com.gestionalmacen.exception.BusinessRuleException;

// Cobro con el codigo QR de MercadoPago (CU-10). El QR esta impreso y pegado en el mostrador,
// asociado a la caja del comercio: lo que cambia en cada venta es la orden que se carga sobre esa caja.
// El sistema no se comunica con ningun equipo, solo con la API.
//
// Se usa la API Instore, no la Orders API nueva: la cuenta del comercio tiene esa ultima bloqueada
// y MercadoPago responde 403 en todos sus endpoints. El flujo de negocio es el mismo.
//
// @Profile("!test"): las pruebas usan la pasarela falsa, asi no dependen de internet ni de credenciales.
@Service
@Profile("!test")
public class MercadoPagoGateway implements IPaymentGateway {

	private static final Logger log = LoggerFactory.getLogger(MercadoPagoGateway.class);
	private static final String API = "https://api.mercadopago.com";
	private static final ParameterizedTypeReference<Map<String, Object>> RESPUESTA = new ParameterizedTypeReference<>() {
	};

	// Las credenciales viven en application-local.properties, que no se sube al repositorio (RNF-02).
	@Value("${mercadopago.access-token:}")
	private String accessToken;

	// Cuenta que recibe el dinero. Va en la direccion de cada pedido.
	@Value("${mercadopago.user-id:}")
	private String userId;

	// Sucursal y caja a las que esta asociado el QR impreso.
	@Value("${mercadopago.store-id:}")
	private String storeId;

	@Value("${mercadopago.pos-external-id:}")
	private String posExternalId;

	// El cobro se identifica con el numero de venta: es unico y no hace falta guardarlo aparte.
	private String referenceOf(Sale sale) {
		return "venta-" + sale.getId();
	}

	@Override
	public String createOrder(Sale sale) {
		String reference = this.referenceOf(sale);
		Map<String, Object> cuerpo = Map.of(
				"external_reference", reference,
				"title", "Venta N° " + sale.getId(),
				"description", "Compra en el almacen",
				"total_amount", sale.getTotal(),
				"items", this.items(sale));

		// Responde 204 sin contenido: el identificador con el que se sigue el cobro es el nuestro.
		this.pedir(() -> this.cliente().put()
				.uri("/instore/qr/seller/collectors/{user}/stores/{store}/pos/{pos}/orders",
						userId, storeId, posExternalId)
				.contentType(MediaType.APPLICATION_JSON)
				.body(cuerpo)
				.retrieve()
				.toBodilessEntity());

		return reference;
	}

	@Override
	public PaymentStatus getOrderStatus(String reference) {
		// La consulta de la orden esta bloqueada para esta cuenta, asi que se mira si ya hay un pago
		// aprobado con esa referencia, que es lo mismo visto desde el otro lado.
		Map<String, Object> respuesta = this.pedir(() -> this.cliente().get()
				.uri("/v1/payments/search?external_reference={ref}", reference)
				.retrieve()
				.body(RESPUESTA));

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> pagos = (List<Map<String, Object>>) respuesta.getOrDefault("results", List.of());
		for (Map<String, Object> pago : pagos) {
			if ("approved".equals(pago.get("status"))) {
				return PaymentStatus.APROBADO;
			}
		}
		// Un intento rechazado no cierra el cobro: el QR sigue abierto y el cliente puede reintentar.
		return PaymentStatus.PENDIENTE;
	}

	@Override
	public void cancelOrder(String reference) {
		// Da de baja la orden abierta en la caja. Hay una sola caja, asi que no hay ambiguedad.
		this.pedir(() -> this.cliente().delete()
				.uri("/instore/qr/seller/collectors/{user}/pos/{pos}/orders", userId, posExternalId)
				.retrieve()
				.toBodilessEntity());
	}

	// El detalle viaja para que el cliente vea en su telefono que esta pagando, y no solo un importe.
	private List<Map<String, Object>> items(Sale sale) {
		List<Map<String, Object>> items = new ArrayList<>();
		for (SaleDetail detail : sale.getDetails()) {
			items.add(Map.of(
					"title", detail.getProductName(),
					"unit_price", detail.getUnitPrice(),
					"quantity", detail.getQuantity(),
					"unit_measure", "unit",
					"total_amount", detail.getSubtotal()));
		}
		return items;
	}

	private RestClient cliente() {
		if (accessToken.isBlank() || userId.isBlank() || storeId.isBlank() || posExternalId.isBlank()) {
			throw new BusinessRuleException(
					"El cobro con MercadoPago no está configurado en este equipo. Cobre en efectivo o por transferencia.");
		}
		return RestClient.builder()
				.baseUrl(API)
				.defaultHeader("Authorization", "Bearer " + accessToken)
				.build();
	}

	// Si MercadoPago no responde o rechaza el pedido, el cajero tiene que ver un mensaje util y no un
	// error 500. El detalle va al log: sin eso, un rechazo de MercadoPago no se puede diagnosticar.
	private <T> T pedir(Supplier<T> llamada) {
		try {
			return llamada.get();
		} catch (HttpStatusCodeException e) {
			// Respondio, pero rechazando. Casi siempre es configuracion: credenciales o permisos de la cuenta.
			log.error("MercadoPago rechazo el pedido con {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
			throw new BusinessRuleException("MercadoPago rechazó el cobro. Revise la configuración o cobre de otra forma.");
		} catch (RestClientException e) {
			log.error("No hubo respuesta de MercadoPago", e);
			throw new BusinessRuleException("No se pudo contactar a MercadoPago. Intente de nuevo o cobre de otra forma.");
		}
	}
}
