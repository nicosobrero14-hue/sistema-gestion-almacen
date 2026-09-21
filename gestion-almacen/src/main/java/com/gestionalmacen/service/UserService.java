package com.gestionalmacen.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.gestionalmacen.dto.UserDTO;
import com.gestionalmacen.entity.Role;
import com.gestionalmacen.entity.User;
import com.gestionalmacen.exception.BusinessRuleException;
import com.gestionalmacen.exception.NotFoundException;
import com.gestionalmacen.repository.IUserRepository;

@Service
public class UserService implements IUserService {

	@Autowired
	private IUserRepository userRepository;

	// Hashea las contraseñas con BCrypt (se configura en PasswordConfig).
	@Autowired
	private PasswordEncoder passwordEncoder;

	@Override
	public List<User> getUsers(String search, boolean activeOnly) {
		return userRepository.search(search.trim(), activeOnly);
	}

	@Override
	public User findUser(Long id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("No existe el usuario con id " + id));
	}

	@Override
	public User saveUser(UserDTO userDTO) {
		if (userRepository.existsByUsername(userDTO.getUsername())) {
			throw new BusinessRuleException("El nombre de usuario " + userDTO.getUsername() + " ya está en uso.");
		}
		// El DTO no la exige porque al modificar puede venir vacia, pero en el alta es obligatoria.
		if (userDTO.getPassword() == null) {
			throw new BusinessRuleException("La contraseña es obligatoria al crear un usuario.");
		}

		User user = new User();
		this.copyData(userDTO, user);
		user.setPasswordHash(passwordEncoder.encode(userDTO.getPassword()));
		return userRepository.save(user);
	}

	@Override
	public User editUser(Long id, UserDTO userDTO) {
		User user = this.findUser(id);

		if (userRepository.existsByUsernameAndIdNot(userDTO.getUsername(), id)) {
			throw new BusinessRuleException("El nombre de usuario " + userDTO.getUsername() + " ya está en uso.");
		}
		// Quitarle el rol al ultimo administrador dejaria el sistema sin nadie que lo gestione.
		if (userDTO.getRole() != Role.ADMIN) {
			this.checkNotLastAdmin(user);
		}

		this.copyData(userDTO, user);
		// Si no vino una contraseña nueva, queda la que tenia.
		if (userDTO.getPassword() != null) {
			user.setPasswordHash(passwordEncoder.encode(userDTO.getPassword()));
		}
		return userRepository.save(user);
	}

	@Override
	public void deactivateUser(Long id) {
		User user = this.findUser(id);
		this.checkNotLastAdmin(user);
		user.setActive(false);
		userRepository.save(user);
	}

	@Override
	public void activateUser(Long id) {
		User user = this.findUser(id);
		user.setActive(true);
		userRepository.save(user);
	}

	// La contraseña no se copia aca: cada metodo decide si corresponde cambiarla.
	private void copyData(UserDTO userDTO, User user) {
		user.setName(userDTO.getName());
		user.setLastName(userDTO.getLastName());
		user.setUsername(userDTO.getUsername());
		user.setRole(userDTO.getRole());
	}

	// CU-18 exc. 3a: el sistema no puede quedarse sin ningun administrador activo.
	private void checkNotLastAdmin(User user) {
		boolean isActiveAdmin = user.getRole() == Role.ADMIN && user.isActive();
		if (isActiveAdmin && userRepository.countByRoleAndActiveTrue(Role.ADMIN) <= 1) {
			throw new BusinessRuleException(user.getUsername()
					+ " es el único administrador activo. El sistema tiene que tener al menos uno.");
		}
	}
}
