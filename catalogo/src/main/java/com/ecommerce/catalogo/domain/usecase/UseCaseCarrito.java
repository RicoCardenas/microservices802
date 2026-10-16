package com.ecommerce.catalogo.domain.usecase;

import com.ecommerce.catalogo.domain.model.*;
import com.ecommerce.catalogo.domain.model.gateway.*;
import java.util.ArrayList;
import java.util.List;

public class UseCaseCarrito {
    private final CarritoGateway carritos;
    private final ProductoGateway productos;
    private final CompraGateway compras;
    private final AuthSaldoGateway auth;
    private final TransactionGateway transactions;

    public UseCaseCarrito(CarritoGateway carritos, ProductoGateway productos, CompraGateway compras,
                          AuthSaldoGateway auth, TransactionGateway transactions) {
        this.carritos = carritos;
        this.productos = productos;
        this.compras = compras;
        this.auth = auth;
        this.transactions = transactions;
    }

    public Resultado<Carrito> consultar(String idUsuario) {
        validarId(idUsuario);
        return transactions.execute(() -> {
            Carrito carrito = carritos.buscar(idUsuario, false);
            return carrito == null ? Resultado.rechazo("Carrito no encontrado") : Resultado.ok("Carrito encontrado", carrito);
        });
    }

    public Resultado<Carrito> agregar(String idUsuario, String idProducto, int cantidad) {
        return modificar(idUsuario, idProducto, cantidad, true);
    }

    public Resultado<Carrito> descontar(String idUsuario, String idProducto, int cantidad) {
        return modificar(idUsuario, idProducto, cantidad, false);
    }

    private Resultado<Carrito> modificar(String idUsuario, String idProducto, int cantidad, boolean agregar) {
        validarId(idUsuario);
        validarId(idProducto);
        if (cantidad <= 0) return Resultado.rechazo("La cantidad debe ser mayor que cero");
        return transactions.execute(() -> {
            if (!carritos.existeUsuario(idUsuario)) return Resultado.rechazo("Usuario no encontrado");
            Carrito carrito = carritos.buscar(idUsuario, agregar);
            if (carrito == null) return Resultado.rechazo("Carrito no encontrado");
            if (carrito.compraPendiente() != null) return Resultado.rechazo("El carrito tiene una compra en proceso");
            if (!agregar && !carrito.activo()) return Resultado.rechazo("El carrito está desactivado");
            Producto producto = productos.buscar(idProducto, true);
            if (producto == null) return Resultado.rechazo("Producto no encontrado");
            if (agregar && !producto.activo()) return Resultado.rechazo("El producto está desactivado");
            List<LineaCarrito> lineas = carrito.activo() ? new ArrayList<>(carrito.productos()) : new ArrayList<>();
            LineaCarrito anterior = null;
            for (LineaCarrito linea : lineas) if (linea.idProducto().equals(idProducto)) anterior = linea;
            long nuevaCantidad = (anterior == null ? 0L : anterior.cantidad()) + (agregar ? cantidad : -(long) cantidad);
            if (agregar && nuevaCantidad > producto.stock()) return Resultado.rechazo("La cantidad solicitada supera el stock disponible");
            if (!agregar && nuevaCantidad < 0) return Resultado.rechazo("La cantidad a descontar supera la cantidad del carrito");
            lineas.remove(anterior);
            if (nuevaCantidad > 0) lineas.add(new LineaCarrito(idProducto, producto.nombre(), (int) nuevaCantidad,
                    anterior == null ? producto.precio() : anterior.precioUnitario()));
            Carrito actualizado = new Carrito(carrito.idCarrito(), idUsuario, true, total(lineas), null, lineas);
            carritos.guardar(actualizado);
            return Resultado.ok(agregar ? "Producto agregado al carrito" : "Cantidad descontada del carrito", actualizado);
        });
    }

    public Resultado<Compra> comprar(String idUsuario, String idCompra) {
        validarId(idUsuario);
        validarId(idCompra);
        Resultado<Compra> preparacion = transactions.execute(() -> prepararCompra(idUsuario, idCompra));
        if (!preparacion.success()) return preparacion;
        Compra compra = preparacion.data();
        if (compra.estado() == Compra.Estado.COMPLETADA) return Resultado.ok("Compra ya completada", compra);
        try {
            CobroResultado cobro = auth.cobrar(compra);
            if (cobro.estado() == CobroResultado.Estado.APROBADO) return confirmar(compra.idCompra());
            Compra cancelada = cancelar(compra.idCompra(), cobro.mensaje(), cobro.estado() == CobroResultado.Estado.RECHAZADO);
            return resultadoCancelacion(cancelada);
        } catch (RuntimeException error) {
            // Si el commit local falló después del cobro, la operación durable permite devolverlo.
            try { cancelar(compra.idCompra(), "No fue posible completar la compra", false); }
            catch (RuntimeException compensacion) { error.addSuppressed(compensacion); }
            throw error;
        }
    }

    private Resultado<Compra> prepararCompra(String idUsuario, String idCompra) {
        compras.bloquearOperacion(idCompra);
        Compra anterior = compras.buscar(idCompra);
        if (anterior != null) {
            if (!anterior.idUsuario().equals(idUsuario)) return Resultado.rechazo("La operación pertenece a otro usuario");
            if (anterior.estado() == Compra.Estado.COMPLETADA) return Resultado.ok("Compra ya completada", anterior);
            return Resultado.rechazo(anterior.estado() == Compra.Estado.PENDIENTE
                    ? "La compra está en proceso" : "La compra fue cancelada; use otro identificador para una nueva compra");
        }
        Carrito carrito = carritos.buscar(idUsuario, false);
        if (carrito == null) return Resultado.rechazo("Carrito no encontrado");
        if (!carrito.activo()) return Resultado.rechazo("El carrito está desactivado");
        if (carrito.compraPendiente() != null) return Resultado.rechazo("El carrito tiene una compra en proceso");
        if (carrito.productos().isEmpty()) return Resultado.rechazo("El carrito está vacío");
        List<LineaCarrito> lineas = ordenadas(carrito);
        List<Producto> inventario = new ArrayList<>();
        for (LineaCarrito linea : lineas) {
            Producto producto = productos.buscar(linea.idProducto(), true);
            if (producto == null || !producto.activo() || producto.stock() < linea.cantidad()) {
                return Resultado.rechazo("Uno de los productos no está disponible o no tiene stock suficiente");
            }
            inventario.add(producto);
        }
        long total = total(lineas);
        Compra compra = new Compra(idCompra, carrito.idCarrito(), idUsuario, total, Compra.Estado.PENDIENTE, "");
        for (int i = 0; i < lineas.size(); i++) {
            Producto producto = inventario.get(i);
            productos.actualizar(producto.conStock(producto.stock() - lineas.get(i).cantidad()));
        }
        compras.guardar(compra);
        carritos.guardar(new Carrito(carrito.idCarrito(), idUsuario, true, total, idCompra, carrito.productos()));
        return Resultado.ok("Compra preparada", compra);
    }

    private Resultado<Compra> confirmar(String idCompra) {
        return transactions.execute(() -> {
            compras.bloquearOperacion(idCompra);
            Compra compra = compras.buscar(idCompra);
            if (compra.estado() == Compra.Estado.COMPLETADA) return Resultado.ok("Compra ya completada", compra);
            if (compra.estado() != Compra.Estado.PENDIENTE) return resultadoCancelacion(compra);
            Carrito carrito = carritos.buscar(compra.idUsuario(), false);
            Compra completada = compra.conEstado(Compra.Estado.COMPLETADA, "");
            carritos.guardar(new Carrito(carrito.idCarrito(), carrito.idUsuario(), false, carrito.total(), null, carrito.productos()));
            compras.guardar(completada);
            return Resultado.ok("Compra completada; carrito desactivado", completada);
        });
    }

    private Compra cancelar(String idCompra, String motivo, boolean cobroRechazado) {
        Compra compra = transactions.execute(() -> {
            compras.bloquearOperacion(idCompra);
            Compra actual = compras.buscar(idCompra);
            if (actual == null || actual.estado() == Compra.Estado.COMPLETADA || actual.estado() == Compra.Estado.CANCELADA) return actual;
            if (actual.estado() == Compra.Estado.PENDIENTE) {
                Carrito carrito = carritos.buscar(actual.idUsuario(), false);
                for (LineaCarrito linea : ordenadas(carrito)) {
                    Producto producto = productos.buscar(linea.idProducto(), true);
                    productos.actualizar(producto.conStock(Math.addExact(producto.stock(), linea.cantidad())));
                }
                carritos.guardar(new Carrito(carrito.idCarrito(), carrito.idUsuario(), true, carrito.total(), null, carrito.productos()));
            }
            Compra cancelada = actual.conEstado(cobroRechazado ? Compra.Estado.CANCELADA : Compra.Estado.CANCELACION_PENDIENTE, motivo);
            compras.guardar(cancelada);
            return cancelada;
        });
        if (compra != null && compra.estado() == Compra.Estado.CANCELACION_PENDIENTE && auth.cancelar(compra)) {
            return transactions.execute(() -> {
                compras.bloquearOperacion(idCompra);
                Compra actual = compras.buscar(idCompra);
                Compra cancelada = actual.conEstado(Compra.Estado.CANCELADA, actual.motivo());
                compras.guardar(cancelada);
                return cancelada;
            });
        }
        return compra;
    }

    public void recuperarCancelacion(String idCompra) {
        cancelar(idCompra, "Compra interrumpida; cancelación y devolución solicitadas", false);
    }

    private Resultado<Compra> resultadoCancelacion(Compra compra) {
        if (compra.estado() == Compra.Estado.COMPLETADA) return Resultado.ok("Compra ya completada", compra);
        return new Resultado<>(false, compra.estado() == Compra.Estado.CANCELACION_PENDIENTE
                ? "Compra rechazada; devolución pendiente de comunicación con Auth" : compra.motivo(), compra);
    }

    private List<LineaCarrito> ordenadas(Carrito carrito) {
        List<LineaCarrito> lineas = new ArrayList<>(carrito.productos());
        lineas.sort((a, b) -> a.idProducto().compareTo(b.idProducto()));
        return lineas;
    }

    private long total(List<LineaCarrito> lineas) {
        long total = 0;
        try {
            for (LineaCarrito linea : lineas) total = Math.addExact(total, linea.subtotal());
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("El total del carrito supera el rango monetario permitido");
        }
        return total;
    }

    private void validarId(String id) {
        if (id == null || id.isBlank() || id.length() > 255) throw new IllegalArgumentException("El identificador es obligatorio y admite máximo 255 caracteres");
    }
}
