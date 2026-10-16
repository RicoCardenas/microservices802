package com.ecommerce.auth.domain.model.gateway;

public interface TransactionGateway {
    <T> T execute(Work<T> work);
    interface Work<T> { T execute(); }
}
