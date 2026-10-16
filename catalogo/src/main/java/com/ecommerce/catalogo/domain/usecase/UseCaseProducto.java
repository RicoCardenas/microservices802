package com.ecommerce.catalogo.domain.usecase;

import com.ecommerce.catalogo.domain.model.*;
import com.ecommerce.catalogo.domain.model.gateway.ProductoGateway;
import com.ecommerce.catalogo.domain.model.gateway.TransactionGateway;

public class UseCaseProducto {
    private final ProductoGateway productos;
    private final TransactionGateway transactions;

    public UseCaseProducto(ProductoGateway productos, TransactionGateway transactions) {
        this.productos = productos;
        this.transactions = transactions;
    }

    public Resultado<Producto> crear(Producto producto) {
        validar(producto);
        if (producto.precio() <= 0 || producto.stock() < 0) return Resultado.rechazo("El precio debe ser positivo y el stock no puede ser negativo");
        if (!productos.existeCatalogo(producto.idCatalogo())) return Resultado.rechazo("Catálogo no encontrado");
        return Resultado.ok("Producto registrado correctamente", productos.crear(producto));
    }

    public Resultado<Producto[]> listar() { return Resultado.ok("Productos encontrados", productos.listar()); }
    public Resultado<Catalogo[]> catalogos() { return Resultado.ok("Catálogos encontrados", productos.catalogos()); }

    public Resultado<Producto> buscar(String idProducto) {
        validarId(idProducto);
        Producto producto = productos.buscar(idProducto, false);
        return producto == null ? Resultado.rechazo("Producto no encontrado") : Resultado.ok("Producto encontrado", producto);
    }

    public Resultado<Producto> actualizar(Producto producto) {
        validar(producto);
        validarId(producto.idProducto());
        if (producto.precio() <= 0 || producto.stock() < 0) return Resultado.rechazo("El precio debe ser positivo y el stock no puede ser negativo");
        return transactions.execute(() -> {
            if (productos.buscar(producto.idProducto(), true) == null) return Resultado.rechazo("Producto no encontrado");
            if (!productos.existeCatalogo(producto.idCatalogo())) return Resultado.rechazo("Catálogo no encontrado");
            productos.actualizar(producto);
            return Resultado.ok("Producto actualizado correctamente", producto);
        });
    }

    public Resultado<Producto> eliminar(String idProducto) {
        validarId(idProducto);
        return transactions.execute(() -> {
            Producto producto = productos.buscar(idProducto, true);
            if (producto == null) return Resultado.rechazo("Producto no encontrado");
            Producto desactivado = new Producto(producto.idProducto(), producto.nombre(), producto.descripcion(),
                    producto.precio(), producto.stock(), false, producto.idCatalogo());
            productos.actualizar(desactivado);
            return Resultado.ok("Producto desactivado; el registro se conserva", desactivado);
        });
    }

    private void validar(Producto producto) {
        if (producto == null || producto.nombre() == null || producto.nombre().isBlank()
                || producto.idCatalogo() == null || producto.idCatalogo().isBlank()) {
            throw new IllegalArgumentException("El nombre y el catálogo son obligatorios");
        }
        if (producto.nombre().length() > 200) throw new IllegalArgumentException("El nombre admite máximo 200 caracteres");
    }

    private void validarId(String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("El identificador del producto es obligatorio");
    }
}
