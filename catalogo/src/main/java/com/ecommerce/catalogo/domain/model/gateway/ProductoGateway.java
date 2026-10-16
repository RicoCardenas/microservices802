package com.ecommerce.catalogo.domain.model.gateway;

import com.ecommerce.catalogo.domain.model.Catalogo;
import com.ecommerce.catalogo.domain.model.Producto;

public interface ProductoGateway {
    Producto buscar(String idProducto, boolean bloquear);
    Producto[] listar();
    Producto crear(Producto producto);
    void actualizar(Producto producto);
    boolean existeCatalogo(String idCatalogo);
    Catalogo[] catalogos();
}
