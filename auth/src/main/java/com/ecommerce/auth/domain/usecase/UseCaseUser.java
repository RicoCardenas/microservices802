package com.ecommerce.auth.domain.usecase;

import com.ecommerce.auth.domain.model.User;
import com.ecommerce.auth.domain.model.UserResult;
import com.ecommerce.auth.domain.model.gateway.EncrypterGateway;
import com.ecommerce.auth.domain.model.gateway.UserGateway;

public class UseCaseUser {

    private final UserGateway userGateway;
    private final EncrypterGateway encrypterGateway;


    public UseCaseUser(UserGateway userGateway, EncrypterGateway encrypterGateway){
        this.userGateway = userGateway;
        this.encrypterGateway = encrypterGateway;
    }

    public UserResult saveUser(User user) {
        validateUserData(user);
        if (user.getAge() < 18) {
            return UserResult.failure("El usuario debe ser mayor de edad");
        }
        normalizeMail(user);

        if (userGateway.emailExists(user.getMail())) {
            return UserResult.failure("Ya existe un usuario registrado con ese correo");
        }

        user.setIdUser(null);
        user.setSaldo(0);
        user.setPassword(encrypterGateway.encrypt(user.getPassword()));
        User savedUser = userGateway.saveUser(user);
        if (savedUser == null || savedUser.getIdUser() == null) {
            throw new IllegalStateException("No se pudo guardar el usuario");
        }
        return UserResult.success("Usuario registrado correctamente", savedUser);
    }

    public UserResult searchUserId(String idUser) {
        validateId(idUser);

        User user = userGateway.searchUserId(idUser);
        if (user == null) {
            return UserResult.failure("Usuario no encontrado");
        }
        return UserResult.success("Usuario encontrado", user);
    }

    public UserResult login(String mail, String password) {
        if (mail == null || mail.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("El correo y la contraseña son obligatorios");
        }

        User user = userGateway.searchUserMail(mail.trim());
        if (user == null || !encrypterGateway.checkPass(password, user.getPassword())) {
            return UserResult.failure("Usuario o contraseña incorrectos");
        }

        return UserResult.success("Inicio de sesión correcto", user);
    }

    public UserResult updateUser(User user) {
        validateUserData(user);
        validateId(user.getIdUser());
        if (user.getAge() < 18) {
            return UserResult.failure("El usuario debe ser mayor de edad");
        }
        normalizeMail(user);

        if (userGateway.emailExistsForAnotherUser(user.getMail(), user.getIdUser())) {
            return UserResult.failure("Ya existe otro usuario registrado con ese correo");
        }

        user.setPassword(encrypterGateway.encrypt(user.getPassword()));
        User updatedUser = userGateway.updateUser(user);
        if (updatedUser == null) {
            return UserResult.failure("Usuario no encontrado");
        }
        return UserResult.success("Usuario actualizado correctamente", updatedUser);
    }

    public UserResult deleteUserId(String idUser) {
        validateId(idUser);

        if (!userGateway.deleteUserId(idUser)) {
            return UserResult.failure("Usuario no encontrado");
        }
        return UserResult.success("Usuario eliminado correctamente", null);
    }

    private void validateUserData(User user) {
        if (user == null || user.getMail() == null || user.getMail().isBlank()
                || user.getPassword() == null || user.getPassword().isBlank()) {
            throw new IllegalArgumentException("El correo y la contraseña son obligatorios");
        }

        if (user.getAge() == null) {
            throw new IllegalArgumentException("La edad es obligatoria");
        }
    }

    private void normalizeMail(User user) {
        user.setMail(user.getMail().trim());
    }

    private void validateId(String idUser) {
        if (idUser == null || idUser.isBlank()) {
            throw new IllegalArgumentException("El identificador del usuario es obligatorio");
        }
    }
}
