package com.ecommerce.catalogo.infrastructure.entry_points;

import com.ecommerce.catalogo.domain.model.Resultado;
import com.ecommerce.catalogo.domain.usecase.UseCaseCarrito;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import static com.ecommerce.catalogo.infrastructure.entry_points.ApiModels.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ecommerce/carrito/{idUsuario}")
public class CarritoController {
    private final UseCaseCarrito useCase;
    @GetMapping
    public Resultado<CarritoResponse> consultar(@PathVariable String idUsuario) {
        var r = useCase.consultar(idUsuario);
        return new Resultado<>(r.success(), r.message(), r.data() == null ? null : CarritoResponse.from(r.data()));
    }
    @PostMapping("/agregar")
    public Resultado<CarritoResponse> agregar(@PathVariable String idUsuario, @RequestBody CantidadRequest request) {
        var r = useCase.agregar(idUsuario, request.idProducto(), request.cantidadObligatoria());
        return new Resultado<>(r.success(), r.message(), r.data() == null ? null : CarritoResponse.from(r.data()));
    }
    @PostMapping("/descontar")
    public Resultado<CarritoResponse> descontar(@PathVariable String idUsuario, @RequestBody CantidadRequest request) {
        var r = useCase.descontar(idUsuario, request.idProducto(), request.cantidadObligatoria());
        return new Resultado<>(r.success(), r.message(), r.data() == null ? null : CarritoResponse.from(r.data()));
    }
    @PostMapping("/comprar")
    public Resultado<CompraResponse> comprar(@PathVariable String idUsuario, @RequestBody CompraRequest request) {
        var r = useCase.comprar(idUsuario, request.idOperacion());
        return new Resultado<>(r.success(), r.message(), r.data() == null ? null : CompraResponse.from(r.data()));
    }
}
