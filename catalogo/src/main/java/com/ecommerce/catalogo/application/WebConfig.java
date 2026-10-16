package com.ecommerce.catalogo.application;

import com.ecommerce.catalogo.infrastructure.driver_adapter.AccesoCatalogo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final AccesoCatalogo acceso;
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(acceso).addPathPatterns("/api/ecommerce/producto/**", "/api/ecommerce/carrito/**");
    }
}
