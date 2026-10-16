package com.ecommerce.catalogo.domain.model;

public record Producto(String idProducto, String nombre, String descripcion, long precio,
                       int stock, boolean activo, String idCatalogo) {
    public Producto conStock(int cantidad) {
        return new Producto(idProducto, nombre, descripcion, precio, cantidad, activo, idCatalogo);
    }
}
