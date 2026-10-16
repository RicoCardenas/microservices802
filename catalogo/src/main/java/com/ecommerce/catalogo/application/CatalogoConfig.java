package com.ecommerce.catalogo.application;

import com.ecommerce.catalogo.domain.model.gateway.*;
import com.ecommerce.catalogo.domain.usecase.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CatalogoConfig {
    @Bean
    public UseCaseProducto useCaseProducto(ProductoGateway productos, TransactionGateway transactions) {
        return new UseCaseProducto(productos, transactions);
    }
    @Bean
    public UseCaseCarrito useCaseCarrito(CarritoGateway carritos, ProductoGateway productos, CompraGateway compras,
                                        AuthSaldoGateway auth, TransactionGateway transactions) {
        return new UseCaseCarrito(carritos, productos, compras, auth, transactions);
    }
}
