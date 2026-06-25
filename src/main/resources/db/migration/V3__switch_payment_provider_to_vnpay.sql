ALTER TABLE orders
    DROP CONSTRAINT IF EXISTS orders_payment_method_check;

ALTER TABLE orders
    ALTER COLUMN payment_method SET DEFAULT 'VNPAY';

UPDATE orders
SET payment_method = 'VNPAY'
WHERE payment_method = 'PAYOS';

ALTER TABLE orders
    ADD CONSTRAINT orders_payment_method_check CHECK (payment_method = 'VNPAY');

ALTER TABLE payments
    DROP CONSTRAINT IF EXISTS payments_provider_check;

ALTER TABLE payments
    ALTER COLUMN provider SET DEFAULT 'VNPAY';

UPDATE payments
SET provider = 'VNPAY'
WHERE provider = 'PAYOS';

ALTER TABLE payments
    RENAME COLUMN payos_order_code TO provider_order_code;

ALTER TABLE payments
    RENAME COLUMN payos_payment_link_id TO provider_payment_link_id;

ALTER TABLE payments
    ADD CONSTRAINT payments_provider_check CHECK (provider = 'VNPAY');

ALTER TABLE payment_events
    ALTER COLUMN provider SET DEFAULT 'VNPAY';

UPDATE payment_events
SET provider = 'VNPAY'
WHERE provider = 'PAYOS';
