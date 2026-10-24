package com.gestionalmacen.service;

// Tope del tamanio de pagina (RF-13).
//
// El tamanio lo pide la pantalla, asi que llega de afuera. Sin un tope, un pedido con un numero
// enorme trae el listado entero y vuelve el problema que la paginacion venia a resolver.
final class Pages {

	private static final int MAX = 100;

	private Pages() {
	}

	// El tamanio pedido, acotado entre uno y el maximo.
	static int size(int size) {
		return Math.min(Math.max(size, 1), MAX);
	}
}
