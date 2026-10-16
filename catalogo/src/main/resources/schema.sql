CREATE SCHEMA IF NOT EXISTS catalogo;;
CREATE TABLE IF NOT EXISTS catalogo.catalogos (
    id_catalogo varchar(255) PRIMARY KEY,
    nombre varchar(200) NOT NULL
);;
INSERT INTO catalogo.catalogos(id_catalogo, nombre) VALUES ('general', 'General') ON CONFLICT DO NOTHING;;
CREATE TABLE IF NOT EXISTS catalogo.productos (
    id_producto varchar(255) PRIMARY KEY,
    id_catalogo varchar(255) NOT NULL REFERENCES catalogo.catalogos(id_catalogo) ON DELETE RESTRICT,
    nombre varchar(200) NOT NULL,
    descripcion text,
    precio bigint NOT NULL CHECK (precio > 0),
    stock integer NOT NULL CHECK (stock >= 0),
    activo boolean NOT NULL DEFAULT true
);;
CREATE TABLE IF NOT EXISTS catalogo.carritos (
    id_carrito varchar(255) PRIMARY KEY,
    id_usuario varchar(255) NOT NULL UNIQUE REFERENCES auth.users(id_user) ON DELETE RESTRICT,
    activo boolean NOT NULL DEFAULT true,
    total bigint NOT NULL DEFAULT 0 CHECK (total >= 0),
    compra_pendiente varchar(255)
);;
CREATE TABLE IF NOT EXISTS catalogo.carrito_productos (
    id_carrito varchar(255) NOT NULL REFERENCES catalogo.carritos(id_carrito) ON DELETE RESTRICT,
    id_producto varchar(255) NOT NULL REFERENCES catalogo.productos(id_producto) ON DELETE RESTRICT,
    cantidad integer NOT NULL CHECK (cantidad > 0),
    precio_unitario bigint NOT NULL CHECK (precio_unitario > 0),
    PRIMARY KEY (id_carrito, id_producto)
);;
CREATE TABLE IF NOT EXISTS catalogo.compras (
    id_compra varchar(255) PRIMARY KEY,
    id_carrito varchar(255) NOT NULL REFERENCES catalogo.carritos(id_carrito) ON DELETE RESTRICT,
    id_usuario varchar(255) NOT NULL REFERENCES auth.users(id_user) ON DELETE RESTRICT,
    total bigint NOT NULL CHECK (total > 0),
    estado varchar(32) NOT NULL CHECK (estado IN ('PENDIENTE', 'COMPLETADA', 'CANCELACION_PENDIENTE', 'CANCELADA')),
    motivo text NOT NULL DEFAULT '',
    creada_en timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP
);;
CREATE INDEX IF NOT EXISTS ix_compras_recuperacion ON catalogo.compras(estado, creada_en);;
-- La aplicación no expone borrado físico. Este trigger también lo impide desde SQL.
CREATE OR REPLACE FUNCTION catalogo.impedir_borrado() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'No se permite eliminar registros de %', TG_TABLE_NAME;
END;
$$;;
CREATE OR REPLACE TRIGGER productos_sin_borrado BEFORE DELETE OR TRUNCATE ON catalogo.productos
FOR EACH STATEMENT EXECUTE FUNCTION catalogo.impedir_borrado();;
CREATE OR REPLACE TRIGGER carritos_sin_borrado BEFORE DELETE OR TRUNCATE ON catalogo.carritos
FOR EACH STATEMENT EXECUTE FUNCTION catalogo.impedir_borrado();;
