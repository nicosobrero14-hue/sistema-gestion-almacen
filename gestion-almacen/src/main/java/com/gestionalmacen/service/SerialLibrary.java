package com.gestionalmacen.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Deja lista la biblioteca nativa que necesita el puerto serie (RF-12 / CU-21).
//
// jSerialComm trae adentro una biblioteca por sistema y arquitectura, y elige cual usar sola.
// En la computadora del comercio elige mal: dice aarch64 teniendo un procesador AMD de 64 bits,
// extrae la version de ARM y despues no puede cargarla. El sintoma es confuso, porque el sistema
// informa que no hay balanza cuando en realidad nunca llego a abrir el puerto.
//
// Aca se resuelve a mano: se busca la biblioteca que corresponde segun lo que informa la propia
// JVM, se la deja en una carpeta nuestra y se le indica a jSerialComm donde esta, con lo que su
// deteccion queda sin usar.
//
// Si no se encuentra una que corresponda, no se toca nada y jSerialComm hace lo que venia
// haciendo: en una computadora donde su deteccion funciona, esto no cambia nada.
final class SerialLibrary {

	private static final Logger log = LoggerFactory.getLogger(SerialLibrary.class);

	// La propiedad con la que jSerialComm acepta una ruta en vez de buscar por su cuenta.
	private static final String PROPIEDAD = "jSerialComm.library.path";

	private SerialLibrary() {
	}

	// Se llama antes de usar el puerto serie por primera vez.
	static void prepare() {
		if (System.getProperty(PROPIEDAD) != null) {
			return; // alguien ya la indico a mano
		}

		String recurso = resourceName();
		if (recurso == null) {
			return;
		}

		try (InputStream origen = SerialLibrary.class.getResourceAsStream(recurso)) {
			if (origen == null) {
				log.warn("El paquete del puerto serie no trae {}", recurso);
				return;
			}

			Path carpeta = Files.createDirectories(
					Paths.get(System.getProperty("java.io.tmpdir"), "gestion-almacen-serie"));
			Path destino = carpeta.resolve(recurso.substring(recurso.lastIndexOf('/') + 1));

			// Si ya esta, se reusa: puede estar abierta por esta misma aplicacion y Windows no
			// deja reemplazar un archivo en uso.
			if (!Files.exists(destino) || Files.size(destino) == 0) {
				Files.copy(origen, destino, StandardCopyOption.REPLACE_EXISTING);
			}
			System.setProperty(PROPIEDAD, carpeta.toString());
		} catch (IOException e) {
			// Sin biblioteca no hay balanza, pero el comercio sigue vendiendo con el peso a mano.
			log.warn("No se pudo preparar la biblioteca del puerto serie: {}", e.getMessage());
		}
	}

	// El archivo que corresponde a este sistema, o null si no se reconoce y conviene no meterse.
	private static String resourceName() {
		String sistema = System.getProperty("os.name", "").toLowerCase();
		String arch = System.getProperty("os.arch", "").toLowerCase();

		if (!sistema.contains("win")) {
			return null;
		}
		return switch (arch) {
			case "amd64", "x86_64" -> "/Windows/x86_64/jSerialComm.dll";
			case "x86", "i386" -> "/Windows/x86/jSerialComm.dll";
			case "aarch64", "arm64" -> "/Windows/aarch64/jSerialComm.dll";
			default -> null;
		};
	}
}
