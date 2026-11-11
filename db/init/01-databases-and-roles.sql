-- Inicialización de las bases del taller 360 Subocol.
--
-- Este script corre UNA sola vez: el entrypoint de PostgreSQL solo ejecuta los
-- archivos de /docker-entrypoint-initdb.d cuando el volumen está vacío.
--
-- Su responsabilidad es acotada: crear las bases y los roles con sus permisos.
-- NO crea tablas, ni índices, ni triggers, ni extensiones: el esquema es de
-- Flyway, dentro de cada servicio (ver requirements.md FR3.1 y FR3.2).
--
-- Roles por base:
--   <dominio>_owner -> dueño del esquema; lo usa solo el proceso de migración.
--   <dominio>_user  -> rol de la aplicación; DML acotado, sin acceso a la otra base.

\set ON_ERROR_STOP on

\getenv orders_owner_password ORDERS_OWNER_PASSWORD
\getenv orders_user_password ORDERS_USER_PASSWORD
\getenv appointment_owner_password APPOINTMENT_OWNER_PASSWORD
\getenv appointment_user_password APPOINTMENT_USER_PASSWORD

-- ---------------------------------------------------------------- roles ----
-- Dueños de esquema (solo migraciones).
SELECT format('CREATE ROLE orders_owner LOGIN PASSWORD %L', :'orders_owner_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'orders_owner')\gexec
SELECT format('ALTER ROLE orders_owner LOGIN PASSWORD %L', :'orders_owner_password')
WHERE EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'orders_owner')\gexec

SELECT format('CREATE ROLE appointment_owner LOGIN PASSWORD %L', :'appointment_owner_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'appointment_owner')\gexec
SELECT format('ALTER ROLE appointment_owner LOGIN PASSWORD %L', :'appointment_owner_password')
WHERE EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'appointment_owner')\gexec

-- Roles de aplicación (DML acotado).
SELECT format('CREATE ROLE orders_user LOGIN PASSWORD %L', :'orders_user_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'orders_user')\gexec
SELECT format('ALTER ROLE orders_user LOGIN PASSWORD %L', :'orders_user_password')
WHERE EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'orders_user')\gexec

SELECT format('CREATE ROLE appointment_user LOGIN PASSWORD %L', :'appointment_user_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'appointment_user')\gexec
SELECT format('ALTER ROLE appointment_user LOGIN PASSWORD %L', :'appointment_user_password')
WHERE EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'appointment_user')\gexec

-- ------------------------------------------------------------- bases ------
-- orders_db ya existe (POSTGRES_DB); appointments_db se crea aquí.
ALTER DATABASE orders_db OWNER TO orders_owner;

SELECT 'CREATE DATABASE appointments_db OWNER appointment_owner'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'appointments_db')\gexec

ALTER DATABASE appointments_db OWNER TO appointment_owner;

-- ----------------------------------------------------------- permisos -----
-- Cada rol de aplicación entra solo a su base. Sin CONNECT cruzado:
-- primero se quita el CONNECT que PostgreSQL concede a PUBLIC por defecto.
REVOKE CONNECT, TEMPORARY ON DATABASE orders_db FROM PUBLIC;
REVOKE CONNECT, TEMPORARY ON DATABASE appointments_db FROM PUBLIC;

REVOKE ALL ON DATABASE orders_db FROM orders_user;
GRANT CONNECT, TEMPORARY ON DATABASE orders_db TO orders_user;
GRANT ALL ON DATABASE orders_db TO orders_owner;

REVOKE ALL ON DATABASE appointments_db FROM appointment_user;
GRANT CONNECT, TEMPORARY ON DATABASE appointments_db TO appointment_user;
GRANT ALL ON DATABASE appointments_db TO appointment_owner;

-- El rol de aplicación usa el esquema público, pero no lo crea ni lo altera.
GRANT USAGE ON SCHEMA public TO orders_user;
GRANT USAGE ON SCHEMA public TO appointment_user;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
