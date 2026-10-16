package com.ecommerce.auth.infrastructure.driver_adapter;

import com.ecommerce.auth.domain.model.gateway.TransactionGateway;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class TransactionGatewayImpl implements TransactionGateway {
    private final TransactionTemplate transaction;

    public TransactionGatewayImpl(PlatformTransactionManager manager) {
        transaction = new TransactionTemplate(manager);
    }

    public <T> T execute(Work<T> work) {
        return transaction.execute(status -> work.execute());
    }
}
