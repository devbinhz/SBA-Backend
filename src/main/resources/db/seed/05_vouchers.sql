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
