package com.ecommerce.catalogo.infrastructure.driver_adapter;

import com.ecommerce.catalogo.domain.model.CobroResultado;
import com.ecommerce.catalogo.domain.model.Compra;
import com.ecommerce.catalogo.domain.model.gateway.AuthSaldoGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.math.BigDecimal;

@Component
public class AuthSaldoGatewayImpl implements AuthSaldoGateway {
    private final RestClient client;

    public AuthSaldoGatewayImpl(@Value("${auth.base-url}") String url, @Value("${auth.internal-token:}") String token) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        client = RestClient.builder().baseUrl(url).requestFactory(factory).defaultHeader("X-Internal-Token", token).build();
    }

    public CobroResultado cobrar(Compra compra) {
        try {
            SaldoResponse respuesta = enviar(compra, "descontar");
            if (respuesta == null || respuesta.success() == null || respuesta.message() == null) {
                return new CobroResultado(CobroResultado.Estado.DESCONOCIDO, "Auth no confirmó el cobro");
            }
            return new CobroResultado(respuesta.success() ? CobroResultado.Estado.APROBADO : CobroResultado.Estado.RECHAZADO, respuesta.message());
        } catch (RestClientException e) {
            return new CobroResultado(CobroResultado.Estado.DESCONOCIDO, "No fue posible comunicarse con Auth");
        }
    }
    public boolean cancelar(Compra compra) {
        try {
            SaldoResponse respuesta = enviar(compra, "cancelar");
            return respuesta != null && Boolean.TRUE.equals(respuesta.success());
        } catch (RestClientException e) { return false; }
    }
    private SaldoResponse enviar(Compra compra, String accion) {
        return client.post().uri("/api/ecommerce/user/{id}/saldo/{accion}", compra.idUsuario(), accion)
                .body(new SaldoRequest(compra.idCompra(), BigDecimal.valueOf(compra.total(), 2)))
                .retrieve().body(SaldoResponse.class);
    }
    private record SaldoRequest(String idOperacion, BigDecimal monto) {}
    private record SaldoResponse(Boolean success, String message, BigDecimal saldo) {}
}
