package com.ecommerce.auth;

import com.ecommerce.auth.domain.model.MovimientoSaldo;
import com.ecommerce.auth.domain.model.gateway.SaldoGateway;
import com.ecommerce.auth.domain.model.gateway.TransactionGateway;
import com.ecommerce.auth.domain.usecase.UseCaseSaldo;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class SaldoTests {
    private final SaldoMemory gateway = new SaldoMemory();
    private final UseCaseSaldo useCase = new UseCaseSaldo(gateway, new TransactionGateway() {
        public <T> T execute(Work<T> work) { return work.execute(); }
    });

    @Test void recargaYCobroSonIdempotentes() {
        assertTrue(useCase.recargar("u1", "r1", 10000).success());
        assertTrue(useCase.recargar("u1", "r1", 10000).success());
        assertEquals(10000L, gateway.saldos.get("u1"));
        assertTrue(useCase.descontar("u1", "c1", 2500).success());
        assertTrue(useCase.descontar("u1", "c1", 2500).success());
        assertEquals(7500L, gateway.saldos.get("u1"));
    }
    @Test void noPermiteSobregiroNiMontosInvalidos() {
        assertFalse(useCase.descontar("u1", "c1", 1).success());
        assertFalse(useCase.recargar("u1", "r1", -1).success());
        assertFalse(useCase.descontar("u1", "c1", 0).success());
        assertEquals(0L, gateway.saldos.get("u1"));
        assertEquals(MovimientoSaldo.Estado.RECHAZADO, gateway.movimientos.get("c1").estado());
    }
    @Test void devuelveUnCobroUnaSolaVez() {
        useCase.recargar("u1", "r1", 10000);
        useCase.descontar("u1", "c1", 2500);
        assertTrue(useCase.cancelar("u1", "c1", 2500).success());
        assertTrue(useCase.cancelar("u1", "c1", 2500).success());
        assertEquals(10000L, gateway.saldos.get("u1"));
        assertFalse(useCase.descontar("u1", "c1", 2500).success());
    }
    @Test void unaCancelacionAnteriorImpideUnCobroTardio() {
        useCase.recargar("u1", "r1", 10000);
        useCase.cancelar("u1", "c1", 2500);
        assertFalse(useCase.descontar("u1", "c1", 2500).success());
        assertEquals(10000L, gateway.saldos.get("u1"));
    }
    @Test void noReutilizaOperacionConOtroUsuarioMontoOTipo() {
        useCase.recargar("u1", "r1", 10000);
        assertFalse(useCase.recargar("u1", "r1", 20000).success());
        assertFalse(useCase.recargar("u2", "r1", 10000).success());
        assertFalse(useCase.descontar("u1", "r1", 10000).success());
        assertFalse(useCase.cancelar("u1", "r1", 10000).success());
        assertEquals(10000L, gateway.saldos.get("u1"));
    }
    @Test void rechazaUsuarioInexistenteYSaldoFueraDeRango() {
        assertFalse(useCase.recargar("unknown", "r1", 100).success());
        gateway.saldos.put("u1", Long.MAX_VALUE);
        assertFalse(useCase.recargar("u1", "r2", 1).success());
        assertThrows(IllegalArgumentException.class, () -> useCase.descontar(" ", "c1", 100));
    }
    private static class SaldoMemory implements SaldoGateway {
        final Map<String, Long> saldos = new HashMap<>(Map.of("u1", 0L, "u2", 0L));
        final Map<String, MovimientoSaldo> movimientos = new HashMap<>();
        public void bloquearOperacion(String id) {}
        public Long bloquearSaldo(String id) { return saldos.get(id); }
        public MovimientoSaldo buscarMovimiento(String id) { return movimientos.get(id); }
        public void guardarSaldo(String id, long saldo) { saldos.put(id, saldo); }
        public void guardarMovimiento(MovimientoSaldo m) { movimientos.put(m.idOperacion(), m); }
    }
}
