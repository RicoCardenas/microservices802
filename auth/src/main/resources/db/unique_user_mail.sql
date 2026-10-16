-- Ejecutar después de resolver las direcciones de correo duplicadas existentes.
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_mail_unique ON auth.users (mail);
