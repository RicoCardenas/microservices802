package com.ecommerce.catalogo.domain.model.gateway;

import com.ecommerce.catalogo.domain.model.Compra;

public interface CompraGateway {
    void bloquearOperacion(String idCompra);
    Compra buscar(String idCompra);
    void guardar(Compra compra);
    String[] cancelacionesPorRecuperar();
}
