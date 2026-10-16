# Pruebas de usuario

1. Iniciar PostgreSQL con la configuración existente y ejecutar `AuthApplication` en IntelliJ con Java 21, conservado en `pom.xml`.
2. Importar `auth.postman_collection.json` en Postman y ejecutar las 16 solicitudes en orden. La colección incluye verificaciones de estado, resultado, mensaje y ausencia de contraseña. Véase la [documentación oficial de importación de Postman](https://learning.postman.com/docs/getting-started/importing-and-exporting/importing-data/).
3. La variable `baseUrl` contiene `http://localhost:8080/api/ecommerce/user`. El primer registro genera `testMail` y guarda `userId` desde `user.idUser`. Las solicitudes 14–16 eliminan y consultan únicamente el usuario creado por la colección.

## Contrato aplicado

Las decisiones están en [`UseCaseUser`](../src/main/java/com/ecommerce/auth/domain/usecase/UseCaseUser.java); [`UserController`](../src/main/java/com/ecommerce/auth/infrastructure/entry_points/UserController.java) convierte sus resultados al siguiente cuerpo:

```json
{
  "success": false,
  "message": "El usuario debe ser mayor de edad",
  "user": null
}
```

| Situación | HTTP | `success` |
| --- | --- | --- |
| Operación completada | 200 | `true` |
| Menor de 18 años, correo duplicado detectado por el caso de uso, credenciales incorrectas o usuario inexistente | 200 | `false` |
| Correo, contraseña, edad o identificador obligatorios ausentes; JSON inválido o tipo incorrecto | 400 | `false` |
| Fallo técnico de persistencia o ejecución | 500 | `false` |
| Método HTTP no admitido / tipo de contenido no admitido | 405 / 415 | Respuesta de Spring MVC |

El HTTP 200 para rechazos de negocio es el contrato solicitado para este proyecto. El cliente debe comprobar `success`. Las definiciones de los estados HTTP se encuentran en [RFC 9110, sección 15](https://www.rfc-editor.org/rfc/rfc9110.html#section-15).

Se mantienen las rutas existentes: `POST /save`, `POST /login`, `GET /search/{idUser}`, `PUT /update` y `DELETE /delete/{idUser}`. El cambio de contrato es que los datos del usuario ahora se encuentran en `user`, en lugar de estar directamente en la raíz del JSON.

## Registro manual

`POST http://localhost:8080/api/ecommerce/user/save`, con `Content-Type: application/json`:

```json
{
  "name": "Ana Perez",
  "mail": "ana.prueba@a.co",
  "password": "Clave123!",
  "rol": "USER",
  "age": 25,
  "phoneNumber": "3001234567"
}
```

Para probar el rechazo de un menor, enviar el mismo cuerpo con `"age": 17`. El caso de uso rechaza el registro antes de persistirlo. Para probar correo duplicado, repetir el registro adulto.

`POST http://localhost:8080/api/ecommerce/user/login`:

```json
{
  "mail": "ana.prueba@a.co",
  "password": "Clave123!"
}
```

## Contraseñas y alcance de la verificación

El registro y la actualización generan un hash BCrypt; el login lo verifica mediante el adaptador `EncrypterGateway`. La implementación sigue los métodos `encode` y `matches` de la [documentación oficial de Spring Security](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html#authentication-password-storage-bcrypt). Se corrigieron la inyección del adaptador, su implementación y el tamaño de la columna de contraseña en `UserData`. La dependencia se limita a `spring-security-crypto`, necesaria para ese adaptador.

Los registros anteriores con contraseñas en texto plano necesitan actualizarse mediante `PUT /update` con sus datos completos y una contraseña en texto plano para generar el hash; también puede crearse un usuario de prueba nuevo. No se modifican automáticamente los registros existentes.

Las pruebas automatizadas cubren el caso de uso, BCrypt y las respuestas MVC con persistencia en memoria. [MockMvc ejecuta el procesamiento de Spring MVC sin un servidor real](https://docs.spring.io/spring-framework/reference/testing/mockmvc.html). No sustituyen una prueba contra PostgreSQL. Los errores técnicos se simulan en las pruebas automatizadas; la colección no provoca fallos en la base de datos.
