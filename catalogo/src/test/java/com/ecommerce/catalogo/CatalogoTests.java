package com.ecommerce.catalogo;

import com.ecommerce.catalogo.domain.model.*;
import com.ecommerce.catalogo.domain.model.gateway.*;
import com.ecommerce.catalogo.domain.usecase.*;
import com.ecommerce.catalogo.infrastructure.entry_points.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CatalogoTests {
    private final Products productos = new Products();
    private final Carts carritos = new Carts();
    private final Purchases compras = new Purchases();
    private final Payment auth = new Payment();
    private final TransactionGateway tx = new TransactionGateway() {
        public <T> T execute(Work<T> work) { return work.execute(); }
    };
    private final UseCaseProducto productoCase = new UseCaseProducto(productos, tx);
    private final UseCaseCarrito carritoCase = new UseCaseCarrito(carritos, productos, compras, auth, tx);

    @Test void crudConservaProductosInclusoSinStock() {
        assertTrue(productoCase.crear(new Producto(null, "Nuevo", "", 1000, 0, true, "general")).success());
        assertEquals(0, productoCase.buscar("nuevo").data().stock());
        assertTrue(productoCase.eliminar("nuevo").success());
        assertNotNull(productos.rows.get("nuevo"));
        assertFalse(productos.rows.get("nuevo").activo());
        assertTrue(productoCase.actualizar(new Producto("nuevo", "Cambio", "", 2000, 5, true, "general")).success());
        assertEquals(2, productoCase.listar().data().length);
    }
    @Test void rechazaPrecioStockYCatalogoInvalidos() {
        assertFalse(productoCase.crear(new Producto(null, "A", "", 0, 1, true, "general")).success());
        assertFalse(productoCase.crear(new Producto(null, "A", "", 1, -1, true, "general")).success());
        assertFalse(productoCase.crear(new Producto(null, "A", "", 1, 1, true, "unknown")).success());
        assertFalse(productoCase.eliminar("unknown").success());
    }
    @Test void acumulaCantidadesYCalculaTotalExacto() {
        assertEquals(2500, carritoCase.agregar("u1", "p1", 2).data().total());
        assertEquals(3750, carritoCase.agregar("u1", "p1", 1).data().total());
        assertEquals(2500, carritoCase.descontar("u1", "p1", 1).data().total());
        assertTrue(carritoCase.descontar("u1", "p1", 2).data().productos().isEmpty());
        assertEquals(0, carritos.rows.get("u1").total());
    }
    @Test void bloqueaCantidadAcumuladaMayorAlStock() {
        carritoCase.agregar("u1", "p1", 4);
        assertFalse(carritoCase.agregar("u1", "p1", 2).success());
        assertEquals(4, carritos.rows.get("u1").productos().getFirst().cantidad());
        assertEquals(5, productos.rows.get("p1").stock());
        assertFalse(carritoCase.descontar("u1", "p1", 5).success());
        assertFalse(carritoCase.agregar("u1", "p1", 0).success());
    }
    @Test void noCreaCarritosHuerfanosNiAgregaProductosInactivos() {
        assertFalse(carritoCase.agregar("unknown", "p1", 1).success());
        assertTrue(carritos.rows.isEmpty());
        productoCase.eliminar("p1");
        assertFalse(carritoCase.agregar("u1", "p1", 1).success());
    }
    @Test void compraDescuentaStockDesactivaCarritoYNoCobraDosVeces() {
        carritoCase.agregar("u1", "p1", 2);
        assertTrue(carritoCase.comprar("u1", "c1").success());
        assertEquals(3, productos.rows.get("p1").stock());
        assertFalse(carritos.rows.get("u1").activo());
        assertTrue(carritoCase.comprar("u1", "c1").success());
        assertEquals(1, auth.cobros);
        assertFalse(carritoCase.comprar("u2", "c1").success());
    }
    @Test void revisaStockOtraVezAntesDeCobrar() {
        carritoCase.agregar("u1", "p1", 5);
        productos.rows.put("p1", productos.rows.get("p1").conStock(2));
        assertFalse(carritoCase.comprar("u1", "c1").success());
        assertEquals(0, auth.cobros);
        assertEquals(2, productos.rows.get("p1").stock());
        assertFalse(carritoCase.comprar("u2", "c2").success());
    }
    @Test void saldoInsuficienteRestauraStockYConservaCarritoActivo() {
        auth.estado = CobroResultado.Estado.RECHAZADO;
        carritoCase.agregar("u1", "p1", 2);
        assertFalse(carritoCase.comprar("u1", "c1").success());
        assertEquals(5, productos.rows.get("p1").stock());
        assertTrue(carritos.rows.get("u1").activo());
        assertEquals(Compra.Estado.CANCELADA, compras.rows.get("c1").estado());
    }
    @Test void perdidaDeRespuestaRechazaYReintentaDevolucionSinReponerStockDosVeces() {
        auth.estado = CobroResultado.Estado.DESCONOCIDO;
        auth.cancelacionDisponible = false;
        carritoCase.agregar("u1", "p1", 2);
        assertFalse(carritoCase.comprar("u1", "c1").success());
        assertEquals(Compra.Estado.CANCELACION_PENDIENTE, compras.rows.get("c1").estado());
        assertEquals(5, productos.rows.get("p1").stock());
        auth.cancelacionDisponible = true;
        carritoCase.recuperarCancelacion("c1");
        carritoCase.recuperarCancelacion("c1");
        assertEquals(Compra.Estado.CANCELADA, compras.rows.get("c1").estado());
        assertEquals(5, productos.rows.get("p1").stock());
        assertTrue(carritos.rows.get("u1").activo());
    }
    @Test void noConfirmaUnaCompraCanceladaDuranteLaLlamadaRemota() {
        carritoCase.agregar("u1", "p1", 2);
        auth.duranteCobro = () -> carritoCase.recuperarCancelacion("c1");
        assertFalse(carritoCase.comprar("u1", "c1").success());
        assertEquals(5, productos.rows.get("p1").stock());
        assertTrue(carritos.rows.get("u1").activo());
    }
    @Test void reutilizaElMismoCarritoParaUnaCompraPosterior() {
        carritoCase.agregar("u1", "p1", 2);
        String id = carritos.rows.get("u1").idCarrito();
        carritoCase.comprar("u1", "c1");
        assertFalse(carritoCase.descontar("u1", "p1", 1).success());
        var carrito = carritoCase.agregar("u1", "p1", 1).data();
        assertEquals(id, carrito.idCarrito());
        assertEquals(1, carrito.productos().getFirst().cantidad());
        assertEquals(1250, carrito.total());
    }
    @Test void conservaElPrecioMostradoAlAgregar() {
        carritoCase.agregar("u1", "p1", 1);
        productoCase.actualizar(new Producto("p1", "A", "", 9999, 5, true, "general"));
        assertEquals(2500, carritoCase.agregar("u1", "p1", 1).data().total());
        assertEquals(2500, carritoCase.comprar("u1", "c1").data().total());
    }
    @Test void mvcMantieneContrato200400YMetodosHttp() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new ProductoController(productoCase), new CarritoController(carritoCase))
                .setControllerAdvice(new ApiExceptionHandler()).build();
        mvc.perform(post("/api/ecommerce/producto/save").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"A\",\"precio\":0,\"stock\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(false));
        mvc.perform(post("/api/ecommerce/producto/save").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"A\",\"precio\":1.001,\"stock\":1}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/ecommerce/carrito/u1/agregar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idProducto\":\"p1\",\"cantidad\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(25.0));
        mvc.perform(get("/api/ecommerce/carrito/u1/comprar")).andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/api/ecommerce/carrito/u1/agregar").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
    private static class Products implements ProductoGateway {
        final Map<String, Producto> rows = new HashMap<>(Map.of("p1", new Producto("p1", "A", "", 1250, 5, true, "general")));
        public Producto buscar(String id, boolean lock) { return rows.get(id); }
        public Producto[] listar() { return rows.values().toArray(Producto[]::new); }
        public Producto crear(Producto p) {
            Producto nuevo = new Producto("nuevo", p.nombre(), p.descripcion(), p.precio(), p.stock(), true, p.idCatalogo());
            rows.put("nuevo", nuevo); return nuevo;
        }
        public void actualizar(Producto p) { rows.put(p.idProducto(), p); }
        public boolean existeCatalogo(String id) { return id.equals("general"); }
        public Catalogo[] catalogos() { return new Catalogo[]{new Catalogo("general", "General")}; }
    }
    private static class Carts implements CarritoGateway {
        final Map<String, Carrito> rows = new HashMap<>();
        public boolean existeUsuario(String id) { return id.equals("u1") || id.equals("u2"); }
        public Carrito buscar(String id, boolean crear) {
            if (crear) rows.putIfAbsent(id, new Carrito("cart-" + id, id, true, 0, null, List.of()));
            return rows.get(id);
        }
        public void guardar(Carrito c) { rows.put(c.idUsuario(), c); }
    }
    private static class Purchases implements CompraGateway {
        final Map<String, Compra> rows = new HashMap<>();
        public void bloquearOperacion(String id) {}
        public Compra buscar(String id) { return rows.get(id); }
        public void guardar(Compra c) { rows.put(c.idCompra(), c); }
        public String[] cancelacionesPorRecuperar() { return new String[0]; }
    }
    private static class Payment implements AuthSaldoGateway {
        CobroResultado.Estado estado = CobroResultado.Estado.APROBADO;
        boolean cancelacionDisponible = true;
        int cobros;
        Runnable duranteCobro = () -> {};
        public CobroResultado cobrar(Compra c) { cobros++; duranteCobro.run(); return new CobroResultado(estado, "Saldo insuficiente o Auth no disponible"); }
        public boolean cancelar(Compra c) { return cancelacionDisponible; }
    }
}
