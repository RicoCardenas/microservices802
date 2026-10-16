package com.ecommerce.auth.domain.model.gateway;

import com.ecommerce.auth.domain.model.MovimientoSaldo;

public interface SaldoGateway {
    void bloquearOperacion(String idOperacion);
    Long bloquearSaldo(String idUsuario);
    MovimientoSaldo buscarMovimiento(String idOperacion);
    void guardarSaldo(String idUsuario, long saldo);
    void guardarMovimiento(MovimientoSaldo movimiento);
}
