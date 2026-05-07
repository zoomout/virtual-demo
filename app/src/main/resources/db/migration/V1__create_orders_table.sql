-- V1__create_orders_table.sql
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY,
    payment_id VARCHAR(255),
    item_id VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    failure_reason VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);
