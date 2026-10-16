package com.ecommerce.catalogo.infrastructure.driver_adapter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;

@Component
public class AccesoCatalogo implements HandlerInterceptor {
    private final RestClient auth;
    private final String token;
    public AccesoCatalogo(@Value("${auth.base-url}") String url, @Value("${auth.internal-token:}") String token) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        auth = RestClient.builder().baseUrl(url).requestFactory(factory).build();
        this.token = token;
    }
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (request.getRequestURI().startsWith("/api/ecommerce/producto")) {
            if (request.getMethod().equals("GET") || request.getMethod().equals("HEAD")) return true;
            if (token.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Falta configurar INTERNAL_API_TOKEN");
            String key = request.getHeader("X-Internal-Token");
            if (key == null || !MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), key.getBytes(StandardCharsets.UTF_8))) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token administrativo inválido");
            }
            return true;
        }
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Basic ")) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Se requiere autenticación Basic con correo y contraseña de Auth");
        String credentials;
        try { credentials = new String(Base64.getDecoder().decode(header.substring(6)), StandardCharsets.UTF_8); }
        catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"); }
        int separator = credentials.indexOf(':');
        if (separator < 1) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        LoginResponse result;
        try {
            result = auth.post().uri("/api/ecommerce/user/login")
                    .body(new LoginRequest(credentials.substring(0, separator), credentials.substring(separator + 1)))
                    .retrieve().body(LoginResponse.class);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No fue posible autenticar con Auth");
        }
        if (result == null || !result.success() || result.user() == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        Object variables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(variables instanceof Map<?, ?> paths) || !result.user().idUser().equals(paths.get("idUsuario"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puede operar el carrito de otro usuario");
        }
        return true;
    }
    private record LoginRequest(String mail, String password) {}
    private record LoginResponse(boolean success, String message, Usuario user) {}
    private record Usuario(String idUser, String name, String mail, String rol, Integer age, String phoneNumber, BigDecimal saldo) {}
}
