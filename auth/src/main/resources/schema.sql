ALTER TABLE auth.users ADD COLUMN IF NOT EXISTS saldo bigint NOT NULL DEFAULT 0;;
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='users_saldo_no_negativo' AND conrelid='auth.users'::regclass) THEN
        ALTER TABLE auth.users ADD CONSTRAINT users_saldo_no_negativo CHECK (saldo >= 0);
    END IF;
END;
$$;;
CREATE TABLE IF NOT EXISTS auth.movimientos_saldo (
    id_operacion varchar(255) PRIMARY KEY,
    id_usuario varchar(255) NOT NULL REFERENCES auth.users(id_user) ON DELETE RESTRICT,
    monto bigint NOT NULL CHECK (monto > 0),
    tipo varchar(16) NOT NULL CHECK (tipo IN ('RECARGA', 'COMPRA')),
    estado varchar(16) NOT NULL CHECK (estado IN ('APROBADO', 'RECHAZADO', 'CANCELADO'))
);;
