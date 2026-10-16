package com.ecommerce.auth.domain.usecase;

import com.ecommerce.auth.domain.model.MovimientoSaldo;
import com.ecommerce.auth.domain.model.SaldoResult;
import com.ecommerce.auth.domain.model.gateway.SaldoGateway;
import com.ecommerce.auth.domain.model.gateway.TransactionGateway;

public class UseCaseSaldo {
    private final SaldoGateway saldoGateway;
    private final TransactionGateway transactions;

    public UseCaseSaldo(SaldoGateway saldoGateway, TransactionGateway transactions) {
        this.saldoGateway = saldoGateway;
        this.transactions = transactions;
    }

    public SaldoResult recargar(String idUsuario, String idOperacion, long monto) {
        return operar(idUsuario, idOperacion, monto, MovimientoSaldo.Tipo.RECARGA, false);
    }

    public SaldoResult descontar(String idUsuario, String idOperacion, long monto) {
        return operar(idUsuario, idOperacion, monto, MovimientoSaldo.Tipo.COMPRA, false);
    }

    public SaldoResult cancelar(String idUsuario, String idOperacion, long monto) {
        return operar(idUsuario, idOperacion, monto, MovimientoSaldo.Tipo.COMPRA, true);
    }

    private SaldoResult operar(String idUsuario, String idOperacion, long monto,
                              MovimientoSaldo.Tipo tipo, boolean cancelar) {
        if (idUsuario == null || idUsuario.isBlank() || idOperacion == null || idOperacion.isBlank()) {
            throw new IllegalArgumentException("El usuario y el identificador de operación son obligatorios");
        }
        if (monto <= 0) return new SaldoResult(false, "El monto debe ser mayor que cero", null);
        return transactions.execute(() -> {
            saldoGateway.bloquearOperacion(idOperacion);
            Long saldo = saldoGateway.bloquearSaldo(idUsuario);
            if (saldo == null) return new SaldoResult(false, "Usuario no encontrado", null);
            MovimientoSaldo anterior = saldoGateway.buscarMovimiento(idOperacion);
            if (anterior != null && (!anterior.idUsuario().equals(idUsuario)
                    || anterior.monto() != monto || anterior.tipo() != tipo)) {
                return new SaldoResult(false, "El identificador de operación ya corresponde a otros datos", saldo);
            }
            if (cancelar) {
                if (anterior != null && anterior.estado() == MovimientoSaldo.Estado.APROBADO) {
                    saldo = Math.addExact(saldo, monto);
                    saldoGateway.guardarSaldo(idUsuario, saldo);
                }
                // La cancelación también se registra si el cobro todavía no llegó.
                saldoGateway.guardarMovimiento(new MovimientoSaldo(idOperacion, idUsuario, monto, tipo,
                        MovimientoSaldo.Estado.CANCELADO));
                return new SaldoResult(true, "Cobro cancelado; cualquier descuento fue devuelto", saldo);
            }
            if (anterior != null) {
                boolean aprobado = anterior.estado() == MovimientoSaldo.Estado.APROBADO;
                return new SaldoResult(aprobado, aprobado ? "Operación ya aplicada" : "Operación rechazada o cancelada", saldo);
            }
            if (tipo == MovimientoSaldo.Tipo.COMPRA && saldo < monto) {
                saldoGateway.guardarMovimiento(new MovimientoSaldo(idOperacion, idUsuario, monto, tipo,
                        MovimientoSaldo.Estado.RECHAZADO));
                return new SaldoResult(false, "Saldo insuficiente", saldo);
            }
            if (tipo == MovimientoSaldo.Tipo.RECARGA && saldo > Long.MAX_VALUE - monto) {
                return new SaldoResult(false, "La recarga supera el saldo máximo permitido", saldo);
            }
            long nuevoSaldo = tipo == MovimientoSaldo.Tipo.RECARGA
                    ? Math.addExact(saldo, monto) : saldo - monto;
            saldoGateway.guardarSaldo(idUsuario, nuevoSaldo);
            saldoGateway.guardarMovimiento(new MovimientoSaldo(idOperacion, idUsuario, monto, tipo,
                    MovimientoSaldo.Estado.APROBADO));
            return new SaldoResult(true, tipo == MovimientoSaldo.Tipo.RECARGA
                    ? "Saldo agregado correctamente" : "Saldo descontado correctamente", nuevoSaldo);
        });
    }
}
