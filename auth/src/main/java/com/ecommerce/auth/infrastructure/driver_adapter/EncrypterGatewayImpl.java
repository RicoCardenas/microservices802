package com.ecommerce.auth.infrastructure.driver_adapter;

import com.ecommerce.auth.domain.model.gateway.EncrypterGateway;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class EncrypterGatewayImpl implements EncrypterGateway {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public String encrypt(String password) {
        return encoder.encode(password);
    }

    @Override
    public Boolean checkPass(String passUser, String passBD) {
        return encoder.matches(passUser,passBD);
    }
}
