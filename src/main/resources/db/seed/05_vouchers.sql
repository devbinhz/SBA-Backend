-- Seed Campaigns
INSERT INTO campaigns (name, campaign_type, is_auto_distributed, start_time, end_time, status, created_at, updated_at)
VALUES
('Welcome Gift Campaign', 'WELCOME_GIFT', true, now() - interval '1 day', now() + interval '365 days', 'ACTIVE', now(), now()),
('Flash Sale 11.11', 'FLASH_SALE', false, now() - interval '1 day', now() + interval '30 days', 'ACTIVE', now(), now())
ON CONFLICT DO NOTHING;

-- Seed Vouchers
INSERT INTO vouchers (campaign_id, code, name, discount_type, discount_value, max_discount_amount, min_order_value, total_quantity, claimed_quantity, start_time, end_time, status, created_at, updated_at)
VALUES
((SELECT id FROM campaigns WHERE name = 'Welcome Gift Campaign' LIMIT 1), 'WELCOME10K', '10,000 VND off for new users', 'FIXED_AMOUNT', 10000, NULL, 0, 1000, 0, now() - interval '1 day', now() + interval '365 days', 'ACTIVE', now(), now()),
((SELECT id FROM campaigns WHERE name = 'Flash Sale 11.11' LIMIT 1), 'FLASH20', '20% off up to 50,000 VND', 'PERCENTAGE', 20, 50000, 150000, 500, 0, now() - interval '1 day', now() + interval '30 days', 'ACTIVE', now(), now()),
(NULL, 'INDEP50K', '50,000 VND off for orders from 500,000 VND', 'FIXED_AMOUNT', 50000, NULL, 500000, 200, 0, now() - interval '1 day', now() + interval '30 days', 'ACTIVE', now(), now())
ON CONFLICT DO NOTHING;

-- A deterministic unused voucher lets the shared customer account demo checkout without relying on VNPay.
INSERT INTO user_vouchers (user_id, voucher_id, status, claimed_at, expires_at, created_at, updated_at)
SELECT
    (SELECT id FROM users WHERE email = 'customer2@gmail.com'),
    (SELECT id FROM vouchers WHERE code = 'INDEP50K' ORDER BY id LIMIT 1),
    'UNUSED',
    now(),
    now() + interval '30 days',
    now(),
    now()
WHERE EXISTS (SELECT 1 FROM users WHERE email = 'customer2@gmail.com')
  AND EXISTS (SELECT 1 FROM vouchers WHERE code = 'INDEP50K')
ON CONFLICT DO NOTHING;

-- A used voucher
INSERT INTO user_vouchers (user_id, voucher_id, status, claimed_at, expires_at, used_at, created_at, updated_at)
SELECT
    (SELECT id FROM users WHERE email = 'customer2@gmail.com'),
    (SELECT id FROM vouchers WHERE code = 'WELCOME10K' ORDER BY id LIMIT 1),
    'USED',
    now() - interval '10 days',
    now() + interval '7 days',
    now() - interval '3 days',
    now() - interval '10 days',
    now() - interval '3 days'
WHERE EXISTS (SELECT 1 FROM users WHERE email = 'customer2@gmail.com')
  AND EXISTS (SELECT 1 FROM vouchers WHERE code = 'WELCOME10K')
ON CONFLICT DO NOTHING;

-- Link the used voucher to a paid order so order detail demonstrates a real discount.
UPDATE orders o
SET user_voucher_id = uv.id,
    discount_amount = 10000,
    total = subtotal + shipping_fee + gift_wrap_fee - 10000,
    updated_at = now()
FROM user_vouchers uv
WHERE o.idempotency_key = 'idempotency-key-seeded-4'
  AND uv.voucher_id = (SELECT id FROM vouchers WHERE code = 'WELCOME10K' LIMIT 1);

UPDATE payments p
SET amount = o.total,
    updated_at = now()
FROM orders o
WHERE p.order_id = o.id
  AND o.idempotency_key = 'idempotency-key-seeded-4';
