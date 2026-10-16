package com.ecommerce.catalogo.domain.model;

public record CobroResultado(Estado estado, String mensaje) {
    public enum Estado { APROBADO, RECHAZADO, DESCONOCIDO }
}
