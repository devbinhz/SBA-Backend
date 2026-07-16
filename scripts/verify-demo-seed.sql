DO $$
DECLARE
    order_state_count integer;
    voucher_state_count integer;
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM users
        WHERE email = 'admin@bookverse.local' AND role = 'ADMIN' AND enabled = true
    ) THEN
        RAISE EXCEPTION 'Demo admin account is missing or invalid';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM users
        WHERE email = 'customer2@gmail.com' AND role = 'CUSTOMER' AND enabled = true
    ) THEN
        RAISE EXCEPTION 'Demo customer account is missing or invalid';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM users
        WHERE email = 'locked.customer@bookverse.local' AND enabled = false
    ) THEN
        RAISE EXCEPTION 'Locked demo customer is missing';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM addresses a
        JOIN users u ON u.id = a.user_id
        WHERE u.email = 'customer2@gmail.com' AND a.is_default = true
    ) THEN
        RAISE EXCEPTION 'Default demo customer address is missing';
    END IF;

    SELECT COUNT(DISTINCT o.status)
    INTO order_state_count
    FROM orders o
    JOIN users u ON u.id = o.user_id
    WHERE u.email = 'customer2@gmail.com'
      AND o.status IN ('PENDING_PAYMENT', 'PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED');

    IF order_state_count <> 6 THEN
        RAISE EXCEPTION 'Expected all 6 order states, found %', order_state_count;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM orders o
        LEFT JOIN (
            SELECT order_id, SUM(line_total) AS line_total
            FROM order_items
            GROUP BY order_id
        ) lines ON lines.order_id = o.id
        WHERE o.idempotency_key LIKE 'idempotency-key-seeded-%'
          AND (
              COALESCE(lines.line_total, 0) <> o.subtotal
              OR o.total <> o.subtotal + o.shipping_fee + o.gift_wrap_fee - o.discount_amount
          )
    ) THEN
        RAISE EXCEPTION 'Seeded order totals are inconsistent';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM orders o
        LEFT JOIN payments p ON p.order_id = o.id
        WHERE o.idempotency_key LIKE 'idempotency-key-seeded-%'
          AND (
              p.id IS NULL
              OR p.amount <> o.total
              OR p.status <> CASE
                  WHEN o.status = 'PENDING_PAYMENT' THEN 'PENDING'
                  WHEN o.status = 'CANCELLED' THEN 'CANCELLED'
                  ELSE 'PAID'
              END
          )
    ) THEN
        RAISE EXCEPTION 'Seeded payment state or amount is inconsistent';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM orders
        WHERE idempotency_key = 'idempotency-key-seeded-3'
          AND delivery_type = 'GIFT' AND gift_wrap_fee = 10000
    ) THEN
        RAISE EXCEPTION 'Gift-delivery demo order is missing';
    END IF;

    SELECT COUNT(DISTINCT uv.status)
    INTO voucher_state_count
    FROM user_vouchers uv
    JOIN users u ON u.id = uv.user_id
    WHERE u.email = 'customer2@gmail.com'
      AND uv.status IN ('UNUSED', 'USED', 'EXPIRED');

    IF voucher_state_count <> 3 THEN
        RAISE EXCEPTION 'Expected all 3 voucher states, found %', voucher_state_count;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM reviews WHERE status = 'PUBLISHED')
       OR NOT EXISTS (SELECT 1 FROM reviews WHERE status = 'HIDDEN')
       OR NOT EXISTS (SELECT 1 FROM review_moderation_history) THEN
        RAISE EXCEPTION 'Review moderation demo data is incomplete';
    END IF;
END $$;

SELECT
    (SELECT COUNT(*) FROM users) AS users,
    (SELECT COUNT(*) FROM addresses) AS addresses,
    (SELECT COUNT(*) FROM orders WHERE idempotency_key LIKE 'idempotency-key-seeded-%') AS seeded_orders,
    (SELECT COUNT(*) FROM payments p JOIN orders o ON o.id = p.order_id
        WHERE o.idempotency_key LIKE 'idempotency-key-seeded-%') AS seeded_payments,
    (SELECT COUNT(*) FROM user_vouchers uv JOIN users u ON u.id = uv.user_id
        WHERE u.email = 'customer2@gmail.com') AS customer_vouchers,
    (SELECT COUNT(*) FROM reviews) AS reviews,
    (SELECT COUNT(*) FROM review_moderation_history) AS moderation_events;
