package com.ecommerce.catalogo.domain.model.gateway;

import com.ecommerce.catalogo.domain.model.CobroResultado;
import com.ecommerce.catalogo.domain.model.Compra;

public interface AuthSaldoGateway {
    CobroResultado cobrar(Compra compra);
    boolean cancelar(Compra compra);
}
