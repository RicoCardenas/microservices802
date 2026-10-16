package com.ecommerce.catalogo.domain.model.gateway;

import com.ecommerce.catalogo.domain.model.Carrito;

public interface CarritoGateway {
    boolean existeUsuario(String idUsuario);
    Carrito buscar(String idUsuario, boolean crear);
    void guardar(Carrito carrito);
}
