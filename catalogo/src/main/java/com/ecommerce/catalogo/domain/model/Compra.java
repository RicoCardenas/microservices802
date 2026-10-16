package com.ecommerce.catalogo.domain.model;

public record Compra(String idCompra, String idCarrito, String idUsuario, long total, Estado estado, String motivo) {
    public enum Estado { PENDIENTE, COMPLETADA, CANCELACION_PENDIENTE, CANCELADA }
    public Compra conEstado(Estado nuevoEstado, String nuevoMotivo) {
        return new Compra(idCompra, idCarrito, idUsuario, total, nuevoEstado, nuevoMotivo);
    }
}
