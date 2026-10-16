package com.ecommerce.auth.application;

import com.ecommerce.auth.domain.model.gateway.EncrypterGateway;
import com.ecommerce.auth.domain.model.gateway.UserGateway;
import com.ecommerce.auth.domain.usecase.UseCaseUser;
import com.ecommerce.auth.domain.usecase.UseCaseSaldo;
import com.ecommerce.auth.domain.model.gateway.SaldoGateway;
import com.ecommerce.auth.domain.model.gateway.TransactionGateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserConfig {

    @Bean
    public UseCaseSaldo useCaseSaldo(SaldoGateway saldoGateway, TransactionGateway transactions) {
        return new UseCaseSaldo(saldoGateway, transactions);
    }

    @Bean
    public UseCaseUser useCaseUser(UserGateway userGateway, EncrypterGateway encrypterGateway){
        return new UseCaseUser(userGateway, encrypterGateway);
    }

}
