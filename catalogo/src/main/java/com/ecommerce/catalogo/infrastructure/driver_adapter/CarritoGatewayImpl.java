package com.ecommerce.catalogo.infrastructure.driver_adapter;

import com.ecommerce.catalogo.domain.model.Carrito;
import com.ecommerce.catalogo.domain.model.LineaCarrito;
import com.ecommerce.catalogo.domain.model.gateway.CarritoGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CarritoGatewayImpl implements CarritoGateway {
    private final JdbcTemplate jdbc;

    public boolean existeUsuario(String idUsuario) {
        return Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from auth.users where id_user=?)", Boolean.class, idUsuario));
    }
    public Carrito buscar(String idUsuario, boolean crear) {
        if (crear) jdbc.update("insert into catalogo.carritos(id_carrito,id_usuario) values (?,?) on conflict(id_usuario) do nothing",
                UUID.randomUUID().toString(), idUsuario);
        return jdbc.query("select * from catalogo.carritos where id_usuario=? for update", (rs, row) -> {
            String id = rs.getString("id_carrito");
            var lineas = jdbc.query("""
                    select d.*, p.nombre from catalogo.carrito_productos d
                    join catalogo.productos p on p.id_producto=d.id_producto
                    where d.id_carrito=? order by d.id_producto
                    """, (item, index) -> new LineaCarrito(item.getString("id_producto"), item.getString("nombre"),
                    item.getInt("cantidad"), item.getLong("precio_unitario")), id);
            return new Carrito(id, idUsuario, rs.getBoolean("activo"), rs.getLong("total"), rs.getString("compra_pendiente"), lineas);
        }, idUsuario).stream().findFirst().orElse(null);
    }
    public void guardar(Carrito carrito) {
        jdbc.update("update catalogo.carritos set activo=?,total=?,compra_pendiente=? where id_carrito=?",
                carrito.activo(), carrito.total(), carrito.compraPendiente(), carrito.idCarrito());
        // Solo se reemplaza el detalle; nunca se elimina el registro del carrito.
        jdbc.update("delete from catalogo.carrito_productos where id_carrito=?", carrito.idCarrito());
        for (LineaCarrito linea : carrito.productos()) jdbc.update("""
                insert into catalogo.carrito_productos(id_carrito,id_producto,cantidad,precio_unitario) values (?,?,?,?)
                """, carrito.idCarrito(), linea.idProducto(), linea.cantidad(), linea.precioUnitario());
    }
}
