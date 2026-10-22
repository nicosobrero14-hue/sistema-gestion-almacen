package com.gestionalmacen.config;

import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Deja servir las fotos de los productos como archivos (RF-14 / CU-22).
// Van fuera del proyecto, en una carpeta del servidor: son datos del comercio, no codigo.
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
	}
}
