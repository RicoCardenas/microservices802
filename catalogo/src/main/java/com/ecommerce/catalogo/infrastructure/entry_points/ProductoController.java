package com.ecommerce.catalogo.infrastructure.entry_points;

import com.ecommerce.catalogo.domain.model.Resultado;
import com.ecommerce.catalogo.domain.usecase.UseCaseProducto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import static com.ecommerce.catalogo.infrastructure.entry_points.ApiModels.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ecommerce/producto")
public class ProductoController {
    private final UseCaseProducto useCase;

    @PostMapping("/save")
    public Resultado<ProductoResponse> crear(@RequestBody ProductoRequest request) {
        var result = useCase.crear(request.toDomain(false));
        return new Resultado<>(result.success(), result.message(), result.data() == null ? null : ProductoResponse.from(result.data()));
    }
    @GetMapping("/list")
    public Resultado<?> listar() {
        var result = useCase.listar();
        return new Resultado<>(result.success(), result.message(), java.util.Arrays.stream(result.data()).map(ProductoResponse::from).toList());
    }
    @GetMapping("/catalogos")
    public Resultado<?> catalogos() { return useCase.catalogos(); }
    @GetMapping("/search/{idProducto}")
    public Resultado<ProductoResponse> buscar(@PathVariable String idProducto) {
        var result = useCase.buscar(idProducto);
        return new Resultado<>(result.success(), result.message(), result.data() == null ? null : ProductoResponse.from(result.data()));
    }
    @PutMapping("/update")
    public Resultado<ProductoResponse> actualizar(@RequestBody ProductoRequest request) {
        var result = useCase.actualizar(request.toDomain(true));
        return new Resultado<>(result.success(), result.message(), result.data() == null ? null : ProductoResponse.from(result.data()));
    }
    @DeleteMapping("/delete/{idProducto}")
    public Resultado<ProductoResponse> eliminar(@PathVariable String idProducto) {
        var result = useCase.eliminar(idProducto);
        return new Resultado<>(result.success(), result.message(), result.data() == null ? null : ProductoResponse.from(result.data()));
    }
}
