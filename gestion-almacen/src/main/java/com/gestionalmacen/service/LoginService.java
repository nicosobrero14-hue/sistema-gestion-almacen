package com.gestionalmacen.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.gestionalmacen.entity.User;
import com.gestionalmacen.repository.IUserRepository;

// Le dice a Spring Security como buscar a un usuario en nuestra tabla (CU-01).
// Esta clase solo lo busca: la contraseña la compara Spring contra el hash.
@Service
public class LoginService implements UserDetailsService {

	@Autowired
	private IUserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		// Mensaje generico a proposito: no hay que confirmar que usuarios existen.
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("Usuario o contraseña incorrectos"));

		// Este User es el de Spring Security, no nuestra entidad.
		return org.springframework.security.core.userdetails.User.builder()
				.username(user.getUsername())
				.password(user.getPasswordHash())
				.roles(user.getRole().name())   // Spring le agrega el prefijo ROLE_
				.disabled(!user.isActive())     // un usuario dado de baja no puede entrar
				.build();
	}
}
