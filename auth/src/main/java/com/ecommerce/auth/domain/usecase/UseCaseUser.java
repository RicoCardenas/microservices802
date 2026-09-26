package com.ecommerce.auth.domain.usecase;

import com.ecommerce.auth.domain.model.User;
import com.ecommerce.auth.domain.model.gateway.UserGateway;

public class UseCaseUser {

    private final UserGateway userGateway;

    public UseCaseUser(UserGateway userGateway){
        this.userGateway = userGateway;
    }

    public User saveUser(User user) {
        validateUserData(user);

        return userGateway.saveUser(user);
    }

    public User searchUserId(String idUser) {
        validateId(idUser);

        return userGateway.searchUserId(idUser);
    }

    public User updateUser(User user) {
        validateUserData(user);
        validateId(user.getIdUser());

        return userGateway.updateUser(user);
    }

    public boolean deleteUserId(String idUser) {
        validateId(idUser);

        return userGateway.deleteUserId(idUser);
    }

    private void validateUserData(User user) {
        if (user == null || user.getMail() == null || user.getPassword() == null) {
            throw new IllegalArgumentException("El correo y la contraseña son obligatorios");
        }

        if (user.getAge() == null || user.getAge() < 18) {
            throw new IllegalArgumentException("El usuario debe ser mayor de edad");
        }
    }

    private void validateId(String idUser) {
        if (idUser == null || idUser.isBlank()) {
            throw new IllegalArgumentException("El identificador del usuario es obligatorio");
        }
    }
}
