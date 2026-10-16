package com.ecommerce.auth.infrastructure.mapper;

import com.ecommerce.auth.domain.model.User;
import com.ecommerce.auth.infrastructure.driver_adapter.UserData;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toUser(UserData userData){

        return new User(
                userData.getIdUser(),
                userData.getName(),
                userData.getMail(),
                userData.getPassword(),
                userData.getRol(),
                userData.getAge(),
                userData.getPhoneNumber(),
                userData.getSaldo()
        );

    }

    public UserData toUserData(User user){

        return new UserData(
                user.getIdUser(),
                user.getName(),
                user.getMail(),
                user.getPassword(),
                user.getRol(),
                user.getAge(),
                user.getPhoneNumber(),
                user.getSaldo()
        );

    }

}
