package com.ecommerce.catalogo.infrastructure.driver_adapter;

import com.ecommerce.catalogo.domain.model.*;
import com.ecommerce.catalogo.domain.model.gateway.ProductoGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProductoGatewayImpl implements ProductoGateway {
    private final JdbcTemplate jdbc;
    private static final RowMapper<Producto> MAPPER = (rs, row) -> new Producto(rs.getString("id_producto"),
            rs.getString("nombre"), rs.getString("descripcion"), rs.getLong("precio"), rs.getInt("stock"),
            rs.getBoolean("activo"), rs.getString("id_catalogo"));

    public Producto buscar(String idProducto, boolean bloquear) {
        return jdbc.query("select * from catalogo.productos where id_producto = ?" + (bloquear ? " for update" : ""),
                MAPPER, idProducto).stream().findFirst().orElse(null);
    }
    public Producto[] listar() {
        return jdbc.query("select * from catalogo.productos order by nombre, id_producto", MAPPER).toArray(Producto[]::new);
    }
    public Producto crear(Producto p) {
        String id = UUID.randomUUID().toString();
        jdbc.update("insert into catalogo.productos(id_producto,nombre,descripcion,precio,stock,activo,id_catalogo) values (?,?,?,?,?,?,?)",
                id, p.nombre(), p.descripcion(), p.precio(), p.stock(), true, p.idCatalogo());
        return buscar(id, false);
    }
    public void actualizar(Producto p) {
        jdbc.update("update catalogo.productos set nombre=?, descripcion=?, precio=?, stock=?, activo=?, id_catalogo=? where id_producto=?",
                p.nombre(), p.descripcion(), p.precio(), p.stock(), p.activo(), p.idCatalogo(), p.idProducto());
    }
    public boolean existeCatalogo(String id) {
        return Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from catalogo.catalogos where id_catalogo=?)", Boolean.class, id));
    }
    public Catalogo[] catalogos() {
        return jdbc.query("select * from catalogo.catalogos order by nombre",
                (rs, row) -> new Catalogo(rs.getString("id_catalogo"), rs.getString("nombre"))).toArray(Catalogo[]::new);
    }
}
