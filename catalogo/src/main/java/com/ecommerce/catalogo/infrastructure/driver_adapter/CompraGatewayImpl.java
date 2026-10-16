package com.ecommerce.catalogo.infrastructure.driver_adapter;

import com.ecommerce.catalogo.domain.model.Compra;
import com.ecommerce.catalogo.domain.model.gateway.CompraGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CompraGatewayImpl implements CompraGateway {
    private final JdbcTemplate jdbc;
    public void bloquearOperacion(String id) {
        jdbc.query("select pg_advisory_xact_lock(hashtextextended(?, 0))", rs -> {}, "compra:" + id);
    }
    public Compra buscar(String id) {
        return jdbc.query("select * from catalogo.compras where id_compra=?", (rs, row) -> new Compra(
                rs.getString("id_compra"), rs.getString("id_carrito"), rs.getString("id_usuario"), rs.getLong("total"),
                Compra.Estado.valueOf(rs.getString("estado")), rs.getString("motivo")), id).stream().findFirst().orElse(null);
    }
    public void guardar(Compra c) {
        jdbc.update("""
                insert into catalogo.compras(id_compra,id_carrito,id_usuario,total,estado,motivo) values (?,?,?,?,?,?)
                on conflict(id_compra) do update set estado=excluded.estado,motivo=excluded.motivo
                """, c.idCompra(), c.idCarrito(), c.idUsuario(), c.total(), c.estado().name(), c.motivo());
    }
    public String[] cancelacionesPorRecuperar() {
        return jdbc.query("""
                select id_compra from catalogo.compras where estado='CANCELACION_PENDIENTE'
                or (estado='PENDIENTE' and creada_en < current_timestamp - interval '60 seconds')
                order by creada_en limit 100
                """, (rs, row) -> rs.getString(1)).toArray(String[]::new);
    }
}
