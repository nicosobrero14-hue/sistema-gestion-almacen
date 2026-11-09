package com.gestionalmacen.config;

import java.io.IOException;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

// Archivos que sirve el backend: las fotos de los productos (RF-14 / CU-22) y, cuando el sistema
// se instala como un solo programa, la pantalla entera.
@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Value("${app.images.folder}")
	private String folder;

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		// noCache no significa descargarla siempre: el navegador pregunta si cambio y, si no cambio,
		// el servidor responde que no y la imagen no viaja. Asi una foto reemplazada se ve enseguida.
		registry.addResourceHandler("/imagenes/**")
				.addResourceLocations(Paths.get(folder).toUri().toString())
				.setCacheControl(CacheControl.noCache());

		// La pantalla es una sola pagina: direcciones como /productos las resuelve el navegador y
		// no el servidor. Si alguien entra directo ahi, o simplemente recarga, el servidor no tiene
		// ese archivo y responderia 404. Se le devuelve index.html y la pantalla se arma sola.
		//
		// Solo hace algo con el frontend empaquetado adentro del jar. Mientras se desarrolla, la
		// pantalla la sirve Vite y por aca no pasa nada.
		registry.addResourceHandler("/**")
				.addResourceLocations("classpath:/static/")
				.resourceChain(true)
				.addResolver(new PathResourceResolver() {

					@Override
					protected Resource getResource(String path, Resource location) throws IOException {
						Resource pedido = location.createRelative(path);
						if (pedido.exists() && pedido.isReadable()) {
							return pedido;
						}

						// Una direccion de la API que no existe tiene que seguir siendo un 404: si se
						// le devolviera la pantalla, el frontend recibiria HTML donde espera un error.
						if (path.startsWith("api/") || path.startsWith("imagenes/")) {
							return null;
						}

						Resource pantalla = new ClassPathResource("/static/index.html");
						return pantalla.exists() ? pantalla : null;
					}
				});
	}
}
