package com.ecommerce.auth.infrastructure.driver_adapter;

import com.ecommerce.auth.domain.model.MovimientoSaldo;
import com.ecommerce.auth.domain.model.gateway.SaldoGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SaldoGatewayImpl implements SaldoGateway {
    private final JdbcTemplate jdbc;

    public void bloquearOperacion(String idOperacion) {
        jdbc.query("select pg_advisory_xact_lock(hashtextextended(?, 0))", rs -> {}, idOperacion);
    }

    public Long bloquearSaldo(String idUsuario) {
        return jdbc.query("select saldo from auth.users where id_user = ? for update",
                (rs, row) -> rs.getLong(1), idUsuario).stream().findFirst().orElse(null);
    }

    public MovimientoSaldo buscarMovimiento(String idOperacion) {
        return jdbc.query("select * from auth.movimientos_saldo where id_operacion = ?",
                (rs, row) -> new MovimientoSaldo(rs.getString("id_operacion"), rs.getString("id_usuario"),
                        rs.getLong("monto"), MovimientoSaldo.Tipo.valueOf(rs.getString("tipo")),
                        MovimientoSaldo.Estado.valueOf(rs.getString("estado"))), idOperacion)
                .stream().findFirst().orElse(null);
    }

    public void guardarSaldo(String idUsuario, long saldo) {
        jdbc.update("update auth.users set saldo = ? where id_user = ?", saldo, idUsuario);
    }

    public void guardarMovimiento(MovimientoSaldo m) {
        jdbc.update("""
                insert into auth.movimientos_saldo(id_operacion, id_usuario, monto, tipo, estado)
                values (?, ?, ?, ?, ?)
                on conflict (id_operacion) do update set estado = excluded.estado
                """, m.idOperacion(), m.idUsuario(), m.monto(), m.tipo().name(), m.estado().name());
    }
}
