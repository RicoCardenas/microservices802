package com.ecommerce.auth.domain.model.gateway;

import com.ecommerce.auth.domain.model.User;

public interface UserGateway {

    User saveUser(User user);

    User searchUserId(String idUser);

    User searchUserMail(String mail);

    boolean emailExists(String mail);

    boolean emailExistsForAnotherUser(String mail, String idUser);

    User updateUser(User user);

    boolean deleteUserId(String idUser);
}
