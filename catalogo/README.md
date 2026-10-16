# Microservicio de Catálogo

Aplicación Maven independiente con Java 21 y Spring Boot 4.1.0, igual que Auth. Las reglas están en `domain/usecase`; Spring, HTTP y SQL se encuentran en `application` e `infrastructure`. El dominio no importa Spring, JDBC, JPA ni bibliotecas externas. Para el detalle del carrito se utiliza `List` y `ArrayList`, como se solicita.

## Inicio en IntelliJ

1. Importar `catalogo/pom.xml` como proyecto o módulo Maven y seleccionar JDK 21.
2. En las configuraciones de ejecución de **ambos** servicios, establecer `INTERNAL_API_TOKEN` con la misma clave privada, larga y aleatoria. No hay una clave predeterminada.
3. Verificar que ambos `application.properties` apunten a la misma base PostgreSQL. La configuración inicial usa `localhost:5432/postgres`.
4. Ejecutar primero `com.ecommerce.auth.AuthApplication` en el puerto **8080** y después `com.ecommerce.catalogo.CatalogoApplication` en **8081**.

Auth crea el esquema `auth` y la tabla `auth.users` mediante Hibernate; después incorpora `saldo` con valor inicial cero y crea `auth.movimientos_saldo`. El esquema predeterminado se establece con `hibernate.default_schema` y su creación automática con `jakarta.persistence.create-database-schemas`, según la [configuración de Hibernate](https://docs.hibernate.org/stable/orm/javadocs/org/hibernate/cfg/SchemaToolingSettings.html#JAKARTA_HBM2DDL_CREATE_SCHEMAS). Catálogo crea el esquema `catalogo` y sus tablas al iniciar. Ambos esquemas pertenecen a la misma base PostgreSQL. Los scripts son idempotentes y no recrean la base de datos ni eliminan los registros existentes. Se utiliza `;;` como separador de inicialización para conservar los bloques de PostgreSQL completos. La configuración de scripts está documentada por [Spring Boot](https://docs.spring.io/spring-boot/how-to/data-initialization.html).

El cambio de `public` a `auth` presupone la eliminación manual, por única vez, de los datos de prueba anteriores, con ambos servicios detenidos. No se migran automáticamente las tablas antiguas de `public`. Después de esa limpieza, iniciar Auth y luego Catálogo; los reinicios posteriores conservan las tablas y los datos.

El adaptador de Catálogo utiliza Spring JDBC para expresar las restricciones, los bloqueos y las transacciones de PostgreSQL de manera explícita. Auth conserva su persistencia JPA y utiliza JDBC para las operaciones transaccionales de saldo. El código SQL permanece fuera del dominio.

## Modelo y reglas

| Tabla | Propósito |
| --- | --- |
| `auth.users` | Usuario de Auth; `saldo` se administra exclusivamente desde Auth |
| `auth.movimientos_saldo` | Registro de recargas, cobros y cancelaciones por identificador único |
| `catalogo.catalogos` | Catálogo; se crea la entrada inicial `general` |
| `catalogo.productos` | Producto, precio, stock y estado activo |
| `catalogo.carritos` | Un carrito por usuario: FK obligatoria y única hacia `users` |
| `catalogo.carrito_productos` | Detalle: producto, cantidad y precio unitario, con ambas FK obligatorias |
| `catalogo.compras` | Estado durable de la compra, necesario para recuperar una devolución interrumpida |

La tabla de detalle permite múltiples productos por carrito y que un producto aparezca en distintos carritos. Las restricciones `NOT NULL`, `UNIQUE`, `CHECK` y las FK se aplican en [schema.sql](src/main/resources/schema.sql), según las garantías descritas por [PostgreSQL](https://www.postgresql.org/docs/current/ddl-constraints.html).

- `DELETE /producto/delete/{id}` cambia `activo` a `false`. Nunca ejecuta un borrado físico del producto. Stock cero conserva el registro y no desactiva el producto automáticamente.
- Los triggers también bloquean `DELETE` y `TRUNCATE` sobre productos y carritos. El detalle sí cambia al añadir o descontar unidades.
- La cantidad acumulada de un producto en el carrito no puede superar el stock disponible. Añadir al carrito no reserva inventario; la compra vuelve a comprobar y bloquear el stock antes de cobrar.
- El precio mostrado al agregar un producto se conserva para esa línea. El total es la suma exacta de precio por cantidad. La API usa importes decimales de máximo dos posiciones; internamente se almacenan centavos enteros, sin `double`.
- Al comprar, el carrito pasa a `Desactivado`. Al añadir un producto después de una compra se reutiliza el mismo carrito, se inicia un detalle vacío y vuelve a `Activo`; esto mantiene la relación 1:1. El registro del carrito no se elimina.
- Las FK impiden eliminar físicamente un usuario que conserve un carrito o movimientos de saldo.

## Compra y recuperación

`UseCaseCarrito` valida el carrito y reserva el stock dentro de una transacción local. Después, `AuthSaldoGatewayImpl`, en infraestructura, consume el endpoint de descuento de Auth. Si el cobro se confirma, otra transacción completa la compra y desactiva el carrito.

Según la opción seleccionada, un fallo de comunicación **rechaza la compra**, repone el inventario y conserva el carrito activo. Si Auth pudo cobrar, se solicita la devolución. Si Auth sigue indisponible, la compra queda en `CANCELACION_PENDIENTE`; una tarea de infraestructura reintenta la devolución cada 10 segundos. Las compras interrumpidas en `PENDIENTE` durante más de 60 segundos también se cancelan y recuperan.

Cada compra utiliza `idOperacion`. Auth conserva el resultado del cobro o su cancelación: repetir la operación no duplica el descuento ni la devolución. Una cancelación anterior al cobro impide que una solicitud tardía lo aplique. Para una nueva compra se debe utilizar otro identificador.

La llamada HTTP entre servicios no forma parte de la transacción local de PostgreSQL: la recuperación se implementa mediante compensación persistente, conforme al [patrón de transacción compensatoria](https://learn.microsoft.com/en-us/azure/architecture/patterns/compensating-transaction). Las operaciones SQL locales utilizan [TransactionTemplate](https://docs.spring.io/spring-framework/reference/data-access/transaction/programmatic.html) y [bloqueos de filas](https://www.postgresql.org/docs/current/explicit-locking.html#LOCKING-ROWS) para evitar sobreventa y actualizaciones de saldo concurrentes incorrectas.

## APIs y Postman

Importar [catalogo.postman_collection.json](postman/catalogo.postman_collection.json), configurar `internalToken` con el valor de `INTERNAL_API_TOKEN` y ejecutar las 22 solicitudes en orden. La colección crea un usuario, recarga saldo, crea un producto, modifica el carrito, compra y verifica saldo, inventario, estados e idempotencia. La importación se describe en la [documentación oficial de Postman](https://learning.postman.com/docs/getting-started/importing-and-exporting/importing-data/).

| Servicio / método | Ruta | Acceso |
| --- | --- | --- |
| Auth `POST` | `/api/ecommerce/user/{idUsuario}/saldo/recargar` | `X-Internal-Token` |
| Auth `POST` | `/api/ecommerce/user/{idUsuario}/saldo/descontar` | `X-Internal-Token`; consumido por Catálogo |
| Auth `POST` | `/api/ecommerce/user/{idUsuario}/saldo/cancelar` | `X-Internal-Token`; compensación |
| Producto `POST` | `/api/ecommerce/producto/save` | `X-Internal-Token` administrativo |
| Producto `PUT` | `/api/ecommerce/producto/update` | `X-Internal-Token` administrativo |
| Producto `DELETE` | `/api/ecommerce/producto/delete/{idProducto}` | `X-Internal-Token` administrativo |
| Producto `GET` | `/api/ecommerce/producto/list` | Público; incluye registros inactivos |
| Producto `GET` | `/api/ecommerce/producto/search/{idProducto}` | Público |
| Producto `GET` | `/api/ecommerce/producto/catalogos` | Público |
| Carrito `GET` | `/api/ecommerce/carrito/{idUsuario}` | Basic Auth del propietario |
| Carrito `POST` | `/api/ecommerce/carrito/{idUsuario}/agregar` | Basic Auth del propietario |
| Carrito `POST` | `/api/ecommerce/carrito/{idUsuario}/descontar` | Basic Auth del propietario |
| Carrito `POST` | `/api/ecommerce/carrito/{idUsuario}/comprar` | Basic Auth del propietario |

Las credenciales Basic son el correo y la contraseña registrados en Auth. Catálogo las valida contra `/api/ecommerce/user/login` y verifica que el identificador de la ruta pertenezca al usuario autenticado. Para un despliegue fuera de localhost, las credenciales y la clave interna deben transportarse sobre HTTPS.

Crear producto:

```json
{"nombre":"Teclado","descripcion":"Mecánico","precio":25000.00,"stock":5,"idCatalogo":"general"}
```

Actualizar producto requiere `idProducto` y `activo`, además de los campos del registro. Añadir o descontar unidades:

```json
{"idProducto":"IDENTIFICADOR_DEL_PRODUCTO","cantidad":2}
```

Recargar o descontar saldo en Auth:

```json
{"idOperacion":"IDENTIFICADOR_UNICO","monto":100000.00}
```

Comprar desde Catálogo:

```json
{"idOperacion":"IDENTIFICADOR_UNICO_DE_LA_COMPRA"}
```

El cliente nunca envía el precio total que se cobrará ni modifica el saldo por el CRUD de usuario. El registro inicia el saldo en cero y la actualización general conserva el saldo existente.

Las respuestas de Catálogo contienen `success`, `message` y `data`; las de saldo contienen `success`, `message` y `saldo`. Los rechazos de negocio usan HTTP 200 con `success: false`; solicitudes inválidas usan 400, accesos inválidos 401/403, fallos técnicos 500 y dependencia de autenticación no disponible 503.

## Verificación reproducible

Con `JAVA_HOME` apuntando al JDK 21, ejecutar `mvn test` dentro de `auth` y `catalogo`. Para verificar HTTP y PostgreSQL, generar primero ambos JAR con `mvn package` y ejecutar desde la raíz:

```bash
python3 catalogo/tests/e2e.py
```

La prueba inicia ambos JAR y una instancia temporal de PostgreSQL en puertos libres, verifica los esquemas `auth` y `catalogo` y sus referencias, simula pérdida de respuesta y recuperación de cobros, verifica compras concurrentes y comprueba que un reinicio conserva tablas y registros. Elimina únicamente su instancia temporal al terminar. No usa la base de datos del proyecto. Requiere los binarios locales de PostgreSQL; se puede indicar su directorio mediante `PG_BIN`. Los resultados y registros quedan en `catalogo/target/e2e`.
