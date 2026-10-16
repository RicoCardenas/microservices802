package com.ecommerce.catalogo.infrastructure.entry_points;

import com.ecommerce.catalogo.domain.model.*;
import java.math.BigDecimal;
import java.util.List;

public final class ApiModels {
    private ApiModels() {}

    public record ProductoRequest(String idProducto, String nombre, String descripcion, BigDecimal precio,
                                  Integer stock, Boolean activo, String idCatalogo) {
        public Producto toDomain(boolean actualizar) {
            if (stock == null) throw new IllegalArgumentException("El stock es obligatorio");
            if (actualizar && activo == null) throw new IllegalArgumentException("El estado activo es obligatorio al actualizar");
            return new Producto(idProducto, nombre, descripcion, centavos(precio), stock,
                    activo == null || activo, idCatalogo == null ? "general" : idCatalogo);
        }
    }
    public record CantidadRequest(String idProducto, Integer cantidad) {
        public int cantidadObligatoria() {
            if (cantidad == null) throw new IllegalArgumentException("La cantidad es obligatoria");
            return cantidad;
        }
    }
    public record CompraRequest(String idOperacion) {}
    public record ProductoResponse(String idProducto, String nombre, String descripcion, BigDecimal precio,
                                   int stock, boolean activo, String idCatalogo) {
        public static ProductoResponse from(Producto p) {
            return new ProductoResponse(p.idProducto(), p.nombre(), p.descripcion(), dinero(p.precio()), p.stock(), p.activo(), p.idCatalogo());
        }
    }
    public record LineaResponse(String idProducto, String nombre, int cantidad, BigDecimal precioUnitario, BigDecimal subtotal) {}
    public record CarritoResponse(String idCarrito, String idUsuario, String estado, BigDecimal total,
                                  String compraPendiente, List<LineaResponse> productos) {
        public static CarritoResponse from(Carrito c) {
            return new CarritoResponse(c.idCarrito(), c.idUsuario(), c.activo() ? "Activo" : "Desactivado", dinero(c.total()),
                    c.compraPendiente(), c.productos().stream().map(l -> new LineaResponse(l.idProducto(), l.nombre(),
                    l.cantidad(), dinero(l.precioUnitario()), dinero(l.subtotal()))).toList());
        }
    }
    public record CompraResponse(String idCompra, String idCarrito, String idUsuario, BigDecimal total, String estado) {
        public static CompraResponse from(Compra c) {
            return new CompraResponse(c.idCompra(), c.idCarrito(), c.idUsuario(), dinero(c.total()), c.estado().name());
        }
    }
    public static BigDecimal dinero(long centavos) { return BigDecimal.valueOf(centavos, 2); }
    public static long centavos(BigDecimal valor) {
        if (valor == null) throw new IllegalArgumentException("El precio es obligatorio");
        try { return valor.movePointRight(2).longValueExact(); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("El precio admite máximo dos decimales y debe estar dentro del rango permitido"); }
    }
}
