package com.gestionalmacen.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.gestionalmacen.dto.LoginDTO;
import com.gestionalmacen.entity.User;
import com.gestionalmacen.service.IUserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

// Inicio y cierre de sesion (CU-01).
// Se usa sesion con cookie: el navegador la manda sola en cada pedido y ningun script la puede leer.
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private IUserService userService;

	//1- iniciar sesion
	@PostMapping("/login")
	public User login(@Valid @RequestBody LoginDTO loginDTO, HttpServletRequest request, HttpServletResponse response) {
		// Compara la contraseña con el hash. Si no coincide, lanza una excepcion que termina en 401 (CU-01 exc. 4a).
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword()));

		// Guarda al usuario en la sesion: asi los pedidos siguientes ya llegan identificados.
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		new HttpSessionSecurityContextRepository().saveContext(context, request, response);

		return userService.findUserByUsername(authentication.getName());
	}

	//2- cerrar sesion: se invalida en el servidor, asi la cookie deja de servir
	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(HttpSession session) {
		session.invalidate();
	}

	//3- quien esta conectado. El frontend lo pregunta al abrir la aplicacion
	@GetMapping("/me")
	public User getSessionUser(Authentication authentication) {
		return userService.findUserByUsername(authentication.getName());
	}
}
