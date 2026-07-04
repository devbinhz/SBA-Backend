INSERT INTO vouchers (name, code_prefix, discount_type, discount_value, tier_min_amount, active, created_at, updated_at)
VALUES
('Giảm 10k cho đơn từ 100k', 'T1', 'FIXED', 10000, 100000, true, now(), now()),
('Giảm 20k cho đơn từ 200k', 'T2', 'FIXED', 20000, 200000, true, now(), now()),
('Giảm 10% cho đơn từ 300k', 'T3', 'PERCENTAGE', 10, 300000, true, now(), now()),
('Giảm 15% cho đơn từ 500k', 'T4', 'PERCENTAGE', 15, 500000, true, now(), now()),
('Giảm 50k cho đơn từ 1 triệu', 'T5', 'FIXED', 50000, 1000000, true, now(), now())
ON CONFLICT DO NOTHING;
