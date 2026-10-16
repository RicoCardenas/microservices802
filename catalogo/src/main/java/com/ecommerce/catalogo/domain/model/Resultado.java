package com.ecommerce.catalogo.domain.model;

public record Resultado<T>(boolean success, String message, T data) {
    public static <T> Resultado<T> ok(String message, T data) { return new Resultado<>(true, message, data); }
    public static <T> Resultado<T> rechazo(String message) { return new Resultado<>(false, message, null); }
}
