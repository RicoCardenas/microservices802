package com.ecommerce.catalogo.domain.model;

public record LineaCarrito(String idProducto, String nombre, int cantidad, long precioUnitario) {
    public long subtotal() { return Math.multiplyExact(precioUnitario, cantidad); }
}
