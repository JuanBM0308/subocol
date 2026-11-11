-- Esquema inicial de orders_db.
--
-- orders_db es propiedad exclusiva de order-service (patron Database per Service).
-- El historial de estados es append-only: el trigger de abajo aborta cualquier
-- UPDATE o DELETE, y los permisos del rol de aplicacion lo complementan.

CREATE TABLE orders (
    id                  uuid         NOT NULL,
    customer_name       varchar(120) NOT NULL,
    vehicle_plate       varchar(16)  NOT NULL,
    vehicle_description varchar(2000) NOT NULL,
    insurer_code        varchar(32),
    status              varchar(32)  NOT NULL,
    created_at          timestamptz  NOT NULL,
    CONSTRAINT pk_orders PRIMARY KEY (id)
);

CREATE TABLE order_status_history (
    id            uuid         NOT NULL,
    event_id      uuid         NOT NULL,
    order_id      uuid         NOT NULL,
    from_status   varchar(32),
    to_status     varchar(32)  NOT NULL,
    reason        varchar(500),
    changed_by    varchar(120) NOT NULL,
    changed_at    timestamptz  NOT NULL,
    event_hash    char(64)     NOT NULL,
    previous_hash char(64)     NOT NULL,
    CONSTRAINT pk_order_status_history PRIMARY KEY (id),
    CONSTRAINT uq_order_status_history_event_id UNIQUE (event_id),
    CONSTRAINT fk_order_status_history_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE INDEX idx_orders_created_at ON orders (created_at DESC);

CREATE INDEX idx_order_status_history_order_changed_at ON order_status_history (order_id, changed_at);

-- El historial es append-only: cualquier UPDATE o DELETE sobre la tabla aborta.
CREATE OR REPLACE FUNCTION order_status_history_append_only() RETURNS trigger
    LANGUAGE plpgsql
    AS $function$
BEGIN
    RAISE EXCEPTION 'order_status_history is append-only: % is not allowed', TG_OP
        USING ERRCODE = 'restrict_violation';
END;
$function$;

CREATE TRIGGER trg_order_status_history_append_only
    BEFORE UPDATE OR DELETE ON order_status_history
    FOR EACH ROW
    EXECUTE FUNCTION order_status_history_append_only();

-- El rol de aplicacion puede existir o no cuando corre la migracion: los
-- permisos solo se aplican si ya esta creado.
DO $do$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'orders_user') THEN
        REVOKE UPDATE, DELETE, TRUNCATE ON order_status_history FROM orders_user;
        GRANT SELECT, INSERT ON order_status_history TO orders_user;
        GRANT SELECT, INSERT, UPDATE ON orders TO orders_user;
    END IF;
END
$do$;
