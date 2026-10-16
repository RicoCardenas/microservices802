package com.ecommerce.auth.domain.model;

public record MovimientoSaldo(String idOperacion, String idUsuario, long monto, Tipo tipo, Estado estado) {
    public enum Tipo { RECARGA, COMPRA }
    public enum Estado { APROBADO, RECHAZADO, CANCELADO }
}
