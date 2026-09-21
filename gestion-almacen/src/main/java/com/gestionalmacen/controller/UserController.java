package com.gestionalmacen.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.gestionalmacen.dto.UserDTO;
import com.gestionalmacen.entity.User;
import com.gestionalmacen.service.IUserService;

import jakarta.validation.Valid;

// API de usuarios (RF-11 / CU-18). Solo la puede usar el administrador (ver SecurityConfig).
@RestController
@RequestMapping("/api/users")
public class UserController {

	@Autowired
	private IUserService userService;

	//1- listar usuarios, con busqueda por nombre, apellido o usuario
	@GetMapping
	public List<User> getUsers(@RequestParam(defaultValue = "") String search,
			@RequestParam(defaultValue = "true") boolean activeOnly) {
		return userService.getUsers(search, activeOnly);
	}

	//2- traer un usuario
	@GetMapping("/{id}")
	public User findUser(@PathVariable Long id) {
		return userService.findUser(id);
	}

	//3- crear un usuario. La contraseña se guarda hasheada
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public User saveUser(@Valid @RequestBody UserDTO userDTO) {
		return userService.saveUser(userDTO);
	}

	//4- modificar un usuario. Si no viene contraseña, se conserva la actual
	@PutMapping("/{id}")
	public User editUser(@PathVariable Long id, @Valid @RequestBody UserDTO userDTO) {
		return userService.editUser(id, userDTO);
	}

	//5- dar de baja: ya no puede entrar, pero no se borra
	@PatchMapping("/{id}/deactivate")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deactivateUser(@PathVariable Long id) {
		userService.deactivateUser(id);
	}

	//6- reactivar un usuario dado de baja
	@PatchMapping("/{id}/activate")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void activateUser(@PathVariable Long id) {
		userService.activateUser(id);
	}
}
