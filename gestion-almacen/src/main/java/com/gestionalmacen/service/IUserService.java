package com.gestionalmacen.service;

import java.util.List;

import com.gestionalmacen.dto.UserDTO;
import com.gestionalmacen.entity.User;

// Operaciones sobre los usuarios del sistema (RF-11 / CU-18).
public interface IUserService {

	List<User> getUsers(String search, boolean activeOnly);

	User findUser(Long id);

	User findUserByUsername(String username);

	User saveUser(UserDTO userDTO);

	User editUser(Long id, UserDTO userDTO);

	void deactivateUser(Long id);

	void activateUser(Long id);
}
