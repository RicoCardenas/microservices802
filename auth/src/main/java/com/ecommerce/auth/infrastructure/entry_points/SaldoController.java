package com.ecommerce.auth.infrastructure.entry_points;

import com.ecommerce.auth.domain.model.SaldoResult;
import com.ecommerce.auth.domain.usecase.UseCaseSaldo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/ecommerce/user/{idUsuario}/saldo")
public class SaldoController {
    private final UseCaseSaldo useCase;
    private final String token;

    public SaldoController(UseCaseSaldo useCase, @Value("${auth.internal-token:}") String token) {
        this.useCase = useCase;
        this.token = token;
    }

    @PostMapping("/recargar")
    public SaldoResponse recargar(@PathVariable String idUsuario, @RequestHeader("X-Internal-Token") String key,
                                 @RequestBody SaldoRequest request) {
        autorizar(key);
        return response(useCase.recargar(idUsuario, request.idOperacion(), centavos(request.monto())));
    }

    @PostMapping("/descontar")
    public SaldoResponse descontar(@PathVariable String idUsuario, @RequestHeader("X-Internal-Token") String key,
                                  @RequestBody SaldoRequest request) {
        autorizar(key);
        return response(useCase.descontar(idUsuario, request.idOperacion(), centavos(request.monto())));
    }

    @PostMapping("/cancelar")
    public SaldoResponse cancelar(@PathVariable String idUsuario, @RequestHeader("X-Internal-Token") String key,
                                 @RequestBody SaldoRequest request) {
        autorizar(key);
        return response(useCase.cancelar(idUsuario, request.idOperacion(), centavos(request.monto())));
    }

    private void autorizar(String key) {
        if (token.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Falta configurar INTERNAL_API_TOKEN");
        if (!MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), key.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token interno inválido");
        }
    }

    private long centavos(BigDecimal monto) {
        if (monto == null) throw new IllegalArgumentException("El monto es obligatorio");
        try { return monto.movePointRight(2).longValueExact(); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("El monto debe tener máximo dos decimales y estar dentro del rango permitido"); }
    }

    private SaldoResponse response(SaldoResult result) {
        return new SaldoResponse(result.success(), result.message(),
                result.saldo() == null ? null : BigDecimal.valueOf(result.saldo(), 2));
    }

    public record SaldoRequest(String idOperacion, BigDecimal monto) {}
    public record SaldoResponse(boolean success, String message, BigDecimal saldo) {}
}
