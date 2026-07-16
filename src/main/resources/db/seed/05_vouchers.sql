INSERT INTO vouchers (name, code_prefix, discount_type, discount_value, tier_min_amount, active, created_at, updated_at)
VALUES
('10,000 VND off orders from 100,000 VND', 'T1', 'FIXED', 10000, 100000, true, now(), now()),
('20,000 VND off orders from 200,000 VND', 'T2', 'FIXED', 20000, 200000, true, now(), now()),
('10% off orders from 300,000 VND', 'T3', 'PERCENTAGE', 10, 300000, true, now(), now()),
('15% off orders from 500,000 VND', 'T4', 'PERCENTAGE', 15, 500000, true, now(), now()),
('50,000 VND off orders from 1,000,000 VND', 'T5', 'FIXED', 50000, 1000000, true, now(), now())
ON CONFLICT DO NOTHING;

-- A deterministic unused voucher lets the shared customer account demo checkout without relying on VNPay.
INSERT INTO user_vouchers (user_id, voucher_id, code, status, expires_at, created_at, updated_at)
SELECT
    (SELECT id FROM users WHERE email = 'customer2@gmail.com'),
    (SELECT id FROM vouchers WHERE code_prefix = 'T2' ORDER BY id LIMIT 1),
    'T2-DEMO-CUSTOMER2',
    'UNUSED',
    now() + interval '30 days',
    now(),
    now()
WHERE EXISTS (SELECT 1 FROM users WHERE email = 'customer2@gmail.com')
  AND EXISTS (SELECT 1 FROM vouchers WHERE code_prefix = 'T2')
ON CONFLICT (code) DO NOTHING;

INSERT INTO user_vouchers (user_id, voucher_id, code, status, expires_at, used_at, created_at, updated_at)
SELECT
    (SELECT id FROM users WHERE email = 'customer2@gmail.com'),
    (SELECT id FROM vouchers WHERE code_prefix = 'T1' ORDER BY id LIMIT 1),
    'T1-DEMO-USED',
    'USED',
    now() + interval '7 days',
    now() - interval '3 days',
    now() - interval '10 days',
    now() - interval '3 days'
WHERE EXISTS (SELECT 1 FROM users WHERE email = 'customer2@gmail.com')
  AND EXISTS (SELECT 1 FROM vouchers WHERE code_prefix = 'T1')
ON CONFLICT (code) DO NOTHING;

INSERT INTO user_vouchers (user_id, voucher_id, code, status, expires_at, created_at, updated_at)
SELECT
    (SELECT id FROM users WHERE email = 'customer2@gmail.com'),
    (SELECT id FROM vouchers WHERE code_prefix = 'T3' ORDER BY id LIMIT 1),
    'T3-DEMO-EXPIRED',
    'EXPIRED',
    now() - interval '1 day',
    now() - interval '31 days',
    now() - interval '1 day'
WHERE EXISTS (SELECT 1 FROM users WHERE email = 'customer2@gmail.com')
  AND EXISTS (SELECT 1 FROM vouchers WHERE code_prefix = 'T3')
ON CONFLICT (code) DO NOTHING;

-- Link the used voucher to a paid order so order detail demonstrates a real discount.
UPDATE orders o
SET user_voucher_id = uv.id,
    discount_amount = 10000,
    total = subtotal + shipping_fee + gift_wrap_fee - 10000,
    updated_at = now()
FROM user_vouchers uv
WHERE o.idempotency_key = 'idempotency-key-seeded-4'
  AND uv.code = 'T1-DEMO-USED';

UPDATE payments p
SET amount = o.total,
    updated_at = now()
FROM orders o
WHERE p.order_id = o.id
  AND o.idempotency_key = 'idempotency-key-seeded-4';
