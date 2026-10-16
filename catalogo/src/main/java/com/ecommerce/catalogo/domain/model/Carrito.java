package com.ecommerce.catalogo.domain.model;

import java.util.List;

public record Carrito(String idCarrito, String idUsuario, boolean activo, long total,
                      String compraPendiente, List<LineaCarrito> productos) {
    public Carrito { productos = List.copyOf(productos); }
}
