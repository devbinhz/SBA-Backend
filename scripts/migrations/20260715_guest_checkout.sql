BEGIN;

-- Guest orders do not belong to a registered user.
ALTER TABLE orders ALTER COLUMN user_id DROP NOT NULL;

-- Protect checkout retries and concurrent submissions from creating duplicates.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_orders_idempotency_key'
          AND conrelid = 'orders'::regclass
    ) THEN
        ALTER TABLE orders
            ADD CONSTRAINT uk_orders_idempotency_key UNIQUE (idempotency_key);
    END IF;
END
$$;

COMMIT;
