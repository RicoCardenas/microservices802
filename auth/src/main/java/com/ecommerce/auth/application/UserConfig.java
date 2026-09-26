package com.ecommerce.auth.application;


import com.ecommerce.auth.domain.model.gateway.UserGateway;
import com.ecommerce.auth.domain.usecase.UseCaseUser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserConfig {

    @Bean
    public UseCaseUser useCaseUser(UserGateway userGateway){
        return new UseCaseUser(userGateway);
    }
}
