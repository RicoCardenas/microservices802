package com.ecommerce.auth.domain.model.gateway;

import com.ecommerce.auth.domain.model.User;

public interface UserGateway {

    User saveUser(User user);

    User searchUserId(String idUser);

    User updateUser(User user);

    boolean deleteUserId(String idUser);

}
