package com.gestionalmacen.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.gestionalmacen.entity.Role;
import com.gestionalmacen.entity.User;
import com.gestionalmacen.repository.IUserRepository;

// Crea el primer administrador cuando la base esta recien instalada (RF-11).
//
// Sin esto no se puede entrar al sistema la primera vez: dar de alta un usuario pide sesion de
// administrador, y para iniciar sesion hace falta un usuario. Hasta ahora el primero salia del
// archivo de datos de prueba, cuya contrasenia esta publicada en el repositorio. Eso sirve para
// desarrollar y no puede llegar al comercio.
//
// Solo hace algo si no hay ningun usuario. En una base con usuarios no toca nada, asi que
// arrancar el sistema todos los dias no tiene efecto, ni siquiera si los datos siguen en el
// archivo de configuracion.
//
// @Profile("!test"): las pruebas crean sus propios usuarios y no necesitan este arranque.
@Component
@Profile("!test")
public class FirstAdmin implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(FirstAdmin.class);

	@Autowired
	private IUserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Value("${app.first-admin.username:}")
	private String username;

	@Value("${app.first-admin.password:}")
	private String password;

	@Value("${app.first-admin.name:Administrador}")
	private String name;

	@Value("${app.first-admin.last-name:del comercio}")
	private String lastName;

	@Override
	public void run(ApplicationArguments args) {
		if (userRepository.count() > 0) {
			return;
		}

		if (username.isBlank() || password.isBlank()) {
			// La base esta vacia y no hay con que entrar: conviene que se vea en la consola, porque
			// desde la pantalla el sintoma seria solamente "usuario o contrasenia incorrectos".
			log.warn("La base no tiene ningun usuario y no hay un primer administrador configurado."
					+ " Complete app.first-admin.username y app.first-admin.password en"
					+ " config/application-local.properties y vuelva a iniciar el sistema.");
			return;
		}

		User admin = new User();
		admin.setName(name);
		admin.setLastName(lastName);
		admin.setUsername(username.trim());
		admin.setPasswordHash(passwordEncoder.encode(password));
		admin.setRole(Role.ADMIN);
		userRepository.save(admin);

		log.info("Se creo el primer administrador \"{}\". Cambie la contrasenia desde el sistema y"
				+ " despues borre app.first-admin.password del archivo de configuracion.", admin.getUsername());
	}
}
