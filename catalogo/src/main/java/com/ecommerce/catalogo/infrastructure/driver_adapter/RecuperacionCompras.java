package com.ecommerce.catalogo.infrastructure.driver_adapter;

import com.ecommerce.catalogo.domain.model.gateway.CompraGateway;
import com.ecommerce.catalogo.domain.usecase.UseCaseCarrito;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecuperacionCompras {
    private static final Logger LOG = LoggerFactory.getLogger(RecuperacionCompras.class);
    private final CompraGateway compras;
    private final UseCaseCarrito useCase;
    @Scheduled(fixedDelay = 10000, initialDelay = 10000)
    public void recuperar() {
        for (String id : compras.cancelacionesPorRecuperar()) {
            try { useCase.recuperarCancelacion(id); }
            catch (RuntimeException e) { LOG.error("No se pudo recuperar la cancelación de la compra {}", id, e); }
        }
    }
}
