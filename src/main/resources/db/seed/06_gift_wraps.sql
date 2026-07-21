-- Sample gift wrap patterns for checkout (deliveryType=GIFT).
-- image_key values must exist in the MinIO thumbnails bucket (same expectation as
-- book cover_key / banner imageKey); upload them via POST /api/v1/admin/uploads/thumbnail
-- before relying on these rows to render a real image in the UI.
INSERT INTO gift_wraps (name, image_key, fee_vnd, display_order, active, created_at, updated_at)
VALUES
('Giấy kraft trơn', 'gift-wrap/kraft-plain.jpg', 10000, 0, true, now(), now()),
('Giấy hoa văn đỏ', 'gift-wrap/red-floral.jpg', 15000, 1, true, now(), now()),
('Giấy caro xanh', 'gift-wrap/blue-checker.jpg', 15000, 2, true, now(), now()),
('Giấy ánh kim sang trọng', 'gift-wrap/luxury-metallic.jpg', 25000, 3, true, now(), now());
