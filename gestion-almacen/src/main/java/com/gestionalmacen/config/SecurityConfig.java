package com.gestionalmacen.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

// Seguridad del sistema (CU-01 / RF-11): quien puede entrar y a que.
@Configuration
public class SecurityConfig {

	// Algoritmo con el que se hashean las contraseñas (RNF-02).
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	// Lo usa el login para comparar el usuario y la contraseña contra la base.
	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		// CSRF: el token viaja en la cookie XSRF-TOKEN y el frontend lo devuelve en la cabecera X-XSRF-TOKEN.
		// Con el nombre en null el token se genera en cada pedido, asi la cookie ya existe al hacer el login.
		CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
		csrfHandler.setCsrfRequestAttributeName(null);

		return http
				.csrf(csrf -> csrf
						.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
						.csrfTokenRequestHandler(csrfHandler))
				.authorizeHttpRequests(requests -> requests
						.requestMatchers("/api/auth/login", "/api/status").permitAll()  // publicos
						.requestMatchers("/api/users/**").hasRole("ADMIN")              // RF-11: usuarios, solo el administrador
						.requestMatchers(HttpMethod.GET, "/api/sales").hasRole("ADMIN") // CU-16: historial de ventas, solo el administrador
						.anyRequest().authenticated())                                  // el resto, cualquiera con sesion
				// Sin sesion responde 401 en vez de redirigir a una pagina de login: esto es una API.
				.exceptionHandling(errors -> errors
						.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
				.build();
	}
}
