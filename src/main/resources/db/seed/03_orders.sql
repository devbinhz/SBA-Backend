INSERT INTO orders (user_id, status, subtotal, shipping_fee, discount_amount, total, address_snapshot, payment_method, idempotency_key, created_at, updated_at, paid_at)
VALUES (
    (SELECT id FROM users WHERE email = 'customer@bookverse.local'),
    'PAID',
    390000,
    30000,
    0,
    420000,
    '{"city": "Hanoi", "line": "123 Main St", "ward": "Ward 1", "phone": "0987654321", "district": "Ba Dinh", "recipient": "BookVerse Customer"}',
    'VNPAY',
    'idempotency-key-seeded-1',
    now(),
    now(),
    now()
);

INSERT INTO order_items (order_id, book_id, title_snapshot, unit_price, quantity, line_total)
VALUES (
    (SELECT currval(pg_get_serial_sequence('orders', 'id'))),
    (SELECT id FROM books WHERE isbn = '9781098107635'),
    'Essential Math for AI: Next-Level Mathematics for Efficient and Successful AI Systems',
    390000,
    1,
    390000
);

INSERT INTO orders (user_id, status, subtotal, shipping_fee, discount_amount, total, address_snapshot, payment_method, idempotency_key, created_at, updated_at, paid_at, delivered_at)
VALUES (
    (SELECT id FROM users WHERE email = 'customer@bookverse.local'),
    'DELIVERED',
    405000,
    30000,
    0,
    435000,
    '{"city": "Hanoi", "line": "123 Main St", "ward": "Ward 1", "phone": "0987654321", "district": "Ba Dinh", "recipient": "BookVerse Customer"}',
    'VNPAY',
    'idempotency-key-seeded-2',
    now(),
    now(),
    now(),
    now()
);

INSERT INTO order_items (order_id, book_id, title_snapshot, unit_price, quantity, line_total)
VALUES (
    (SELECT currval(pg_get_serial_sequence('orders', 'id'))),
    (SELECT id FROM books WHERE isbn = '9781805127857'),
    'FastAPI Cookbook: Develop high-performance APIs and web applications with Python',
    405000,
    1,
    405000
);

-- Complete the delivered purchase history used by the review demo dataset.
INSERT INTO order_items (order_id, book_id, title_snapshot, unit_price, quantity, line_total)
VALUES
    ((SELECT id FROM orders WHERE idempotency_key = 'idempotency-key-seeded-2'),
     (SELECT id FROM books WHERE isbn = '9781883629007'),
     '101 Creative Problem Solving Techniques: The Handbook of New Ideas for Business', 75000, 1, 75000),
    ((SELECT id FROM orders WHERE idempotency_key = 'idempotency-key-seeded-2'),
     (SELECT id FROM books WHERE isbn = '9781098166304'),
     'AI Engineering: Building Applications with Foundation Models', 90000, 1, 90000),
    ((SELECT id FROM orders WHERE idempotency_key = 'idempotency-key-seeded-2'),
     (SELECT id FROM books WHERE isbn = '9780470187715'),
     'A Trader''s Money Management System: How to Ensure Profit and Avoid the Risk of Ruin', 105000, 1, 105000),
    ((SELECT id FROM orders WHERE idempotency_key = 'idempotency-key-seeded-2'),
     (SELECT id FROM books WHERE isbn = '9781626251700'),
     'Adult Children of Emotionally Immature Parents', 120000, 1, 120000);

UPDATE orders
SET subtotal = 795000,
    total = 825000,
    updated_at = now()
WHERE idempotency_key = 'idempotency-key-seeded-2';

INSERT INTO orders (user_id, status, subtotal, shipping_fee, delivery_type, gift_wrap_fee,
                    discount_amount, total, address_snapshot, payment_method, idempotency_key,
                    created_at, updated_at, paid_at, delivered_at)
VALUES (
    (SELECT id FROM users WHERE email = 'customer2@gmail.com'),
    'DELIVERED',
    1560000,
    30000,
    'GIFT',
    10000,
    0,
    1600000,
    '{"city": "Ho Chi Minh City", "line": "456 Demo Street", "ward": "Ward 1", "phone": "0900000000", "district": "District 1", "recipient": "Customer2"}',
    'VNPAY',
    'idempotency-key-seeded-3',
    now(),
    now(),
    now(),
    now()
);

INSERT INTO order_items (order_id, book_id, title_snapshot, unit_price, quantity, line_total)
VALUES
    ((SELECT id FROM orders WHERE idempotency_key = 'idempotency-key-seeded-3'),
     (SELECT id FROM books WHERE isbn = '9781883629007'),
     '101 Creative Problem Solving Techniques: The Handbook of New Ideas for Business', 75000, 1, 75000),
    ((SELECT id FROM orders WHERE idempotency_key = 'idempotency-key-seeded-3'),
     (SELECT id FROM books WHERE isbn = '9781098166304'),
     'AI Engineering: Building Applications with Foundation Models', 90000, 1, 90000),
    ((SELECT id FROM orders WHERE idempotency_key = 'idempotency-key-seeded-3'),
     (SELECT id FROM books WHERE isbn = '9781626251700'),
     'Adult Children of Emotionally Immature Parents', 120000, 1, 120000),
    ((SELECT id FROM orders WHERE idempotency_key = 'idempotency-key-seeded-3'),
     (SELECT id FROM books WHERE isbn = '9780300273601'),
     'Attacking the Elites', 150000, 1, 150000),
    ((SELECT id FROM orders WHERE idempotency_key = 'idempotency-key-seeded-3'),
     (SELECT id FROM books WHERE isbn = '9781098107635'),
     'Essential Math for AI', 390000, 1, 390000),
    ((SELECT id FROM orders WHERE idempotency_key = 'idempotency-key-seeded-3'),
     (SELECT id FROM books WHERE isbn = '9781805127857'),
     'FastAPI Cookbook', 405000, 1, 405000),
    ((SELECT id FROM orders WHERE idempotency_key = 'idempotency-key-seeded-3'),
     (SELECT id FROM books WHERE title = 'Cây cỏ Việt Nam (An Illustrated Flora of Vietnam) - Quyển I'),
     'Cây cỏ Việt Nam (An Illustrated Flora of Vietnam) - Quyển I', 135000, 1, 135000),
    ((SELECT id FROM orders WHERE idempotency_key = 'idempotency-key-seeded-3'),
     (SELECT id FROM books WHERE title = 'Biophysical Chemistry'),
     'Biophysical Chemistry', 195000, 1, 195000);

-- Additional customer2 orders provide stable order-list states for the demo.
INSERT INTO orders (user_id, status, subtotal, shipping_fee, discount_amount, total, address_snapshot,
                    payment_method, idempotency_key, created_at, updated_at, paid_at)
VALUES (
    (SELECT id FROM users WHERE email = 'customer2@gmail.com'),
    'PAID',
    150000,
    30000,
    0,
    180000,
    '{"city": "Ho Chi Minh City", "line": "456 Demo Street", "ward": "Ward 1", "phone": "0900000000", "district": "District 1", "recipient": "Customer2"}',
    'VNPAY',
    'idempotency-key-seeded-4',
    now() - interval '3 days',
    now() - interval '3 days',
    now() - interval '3 days'
);

INSERT INTO order_items (order_id, book_id, title_snapshot, unit_price, quantity, line_total)
VALUES (
    (SELECT id FROM orders WHERE idempotency_key = 'idempotency-key-seeded-4'),
    (SELECT id FROM books WHERE isbn = '9780300273601'),
    'Attacking the Elites',
    150000,
    1,
    150000
);

INSERT INTO orders (user_id, status, subtotal, shipping_fee, discount_amount, total, address_snapshot,
                    payment_method, idempotency_key, created_at, updated_at, paid_at)
VALUES (
    (SELECT id FROM users WHERE email = 'customer2@gmail.com'),
    'PROCESSING',
    495000,
    30000,
    0,
    525000,
    '{"city": "Ho Chi Minh City", "line": "456 Demo Street", "ward": "Ward 1", "phone": "0900000000", "district": "District 1", "recipient": "Customer2"}',
    'VNPAY',
    'idempotency-key-seeded-5',
    now() - interval '2 days',
    now() - interval '2 days',
    now() - interval '2 days'
);

INSERT INTO order_items (order_id, book_id, title_snapshot, unit_price, quantity, line_total)
VALUES
    ((SELECT id FROM orders WHERE idempotency_key = 'idempotency-key-seeded-5'),
     (SELECT id FROM books WHERE isbn = '9781098166304'),
     'AI Engineering: Building Applications with Foundation Models', 90000, 1, 90000),
    ((SELECT id FROM orders WHERE idempotency_key = 'idempotency-key-seeded-5'),
     (SELECT id FROM books WHERE isbn = '9781805127857'),
     'FastAPI Cookbook', 405000, 1, 405000);

INSERT INTO orders (user_id, status, subtotal, shipping_fee, discount_amount, total, address_snapshot,
                    payment_method, idempotency_key, shipping_provider, tracking_code,
                    created_at, updated_at, paid_at, shipped_at)
VALUES (
    (SELECT id FROM users WHERE email = 'customer2@gmail.com'),
    'SHIPPED',
    390000,
    30000,
    0,
    420000,
    '{"city": "Ho Chi Minh City", "line": "456 Demo Street", "ward": "Ward 1", "phone": "0900000000", "district": "District 1", "recipient": "Customer2"}',
    'VNPAY',
    'idempotency-key-seeded-6',
    'GHN',
    'BV-DEMO-0006',
    now() - interval '1 day',
    now() - interval '12 hours',
    now() - interval '1 day',
    now() - interval '12 hours'
);

INSERT INTO order_items (order_id, book_id, title_snapshot, unit_price, quantity, line_total)
VALUES (
    (SELECT id FROM orders WHERE idempotency_key = 'idempotency-key-seeded-6'),
    (SELECT id FROM books WHERE isbn = '9781098107635'),
    'Essential Math for AI',
    390000,
    1,
    390000
);

-- Pending and cancelled orders complete the customer order-state matrix.
INSERT INTO orders (user_id, status, subtotal, shipping_fee, discount_amount, total, address_snapshot,
                    payment_method, idempotency_key, expires_at, created_at, updated_at)
VALUES (
    (SELECT id FROM users WHERE email = 'customer2@gmail.com'),
    'PENDING_PAYMENT',
    75000,
    30000,
    0,
    105000,
    '{"city": "Ho Chi Minh City", "line": "456 Demo Street", "ward": "Ben Nghe Ward", "phone": "0900000000", "district": "District 1", "recipient": "Customer2"}',
    'VNPAY',
    'idempotency-key-seeded-7',
        now() + interval '7 days',
    now() - interval '1 minute',
    now() - interval '1 minute'
);

INSERT INTO order_items (order_id, book_id, title_snapshot, unit_price, quantity, line_total)
VALUES (
    (SELECT id FROM orders WHERE idempotency_key = 'idempotency-key-seeded-7'),
    (SELECT id FROM books WHERE isbn = '9781883629007'),
    '101 Creative Problem Solving Techniques: The Handbook of New Ideas for Business',
    75000,
    1,
    75000
);

INSERT INTO orders (user_id, status, subtotal, shipping_fee, discount_amount, total, address_snapshot,
                    payment_method, idempotency_key, expires_at, created_at, updated_at, cancelled_at)
VALUES (
    (SELECT id FROM users WHERE email = 'customer2@gmail.com'),
    'CANCELLED',
    150000,
    30000,
    0,
    180000,
    '{"city": "Ho Chi Minh City", "line": "456 Demo Street", "ward": "Ben Nghe Ward", "phone": "0900000000", "district": "District 1", "recipient": "Customer2"}',
    'VNPAY',
    'idempotency-key-seeded-8',
    now() - interval '1 day',
    now() - interval '1 day 20 minutes',
    now() - interval '1 day',
    now() - interval '1 day'
);

INSERT INTO order_items (order_id, book_id, title_snapshot, unit_price, quantity, line_total)
VALUES (
    (SELECT id FROM orders WHERE idempotency_key = 'idempotency-key-seeded-8'),
    (SELECT id FROM books WHERE isbn = '9780300273601'),
    'Attacking the Elites',
    150000,
    1,
    150000
);

-- Every seeded order has a matching payment record.
INSERT INTO payments (order_id, provider, status, amount, provider_order_code, transaction_id, paid_at, created_at, updated_at)
SELECT id, 'VNPAY', 'PAID', total, id * 1000 + 1, 'DEMO-' || id, paid_at, now(), now()
FROM orders
WHERE idempotency_key IN (
    'idempotency-key-seeded-1', 'idempotency-key-seeded-2', 'idempotency-key-seeded-3',
    'idempotency-key-seeded-4', 'idempotency-key-seeded-5', 'idempotency-key-seeded-6'
);

INSERT INTO payments (order_id, provider, status, amount, provider_order_code, provider_payment_link_id,
                      checkout_url, created_at, updated_at)
SELECT id, 'VNPAY', 'PENDING', total, id * 1000 + 1, 'DEMO-PENDING-LINK',
       'https://sandbox.vnpayment.vn/paymentv2/vpcpay.html', now(), now()
FROM orders
WHERE idempotency_key = 'idempotency-key-seeded-7';

INSERT INTO payments (order_id, provider, status, amount, provider_order_code, created_at, updated_at)
SELECT id, 'VNPAY', 'CANCELLED', total, id * 1000 + 1, now(), now()
FROM orders
WHERE idempotency_key = 'idempotency-key-seeded-8';

INSERT INTO order_status_history (order_id, from_status, to_status, changed_by, note, created_at)
SELECT id, NULL, status, user_id, 'Created by deterministic demo seed', now()
FROM orders
WHERE idempotency_key IN (
    'idempotency-key-seeded-1', 'idempotency-key-seeded-2', 'idempotency-key-seeded-3',
    'idempotency-key-seeded-4', 'idempotency-key-seeded-5', 'idempotency-key-seeded-6',
    'idempotency-key-seeded-7', 'idempotency-key-seeded-8'
);

UPDATE books b
SET sold_count = delivered.total_sold,
    updated_at = now()
FROM (
    SELECT oi.book_id, SUM(oi.quantity)::integer AS total_sold
    FROM order_items oi
    JOIN orders o ON o.id = oi.order_id
    WHERE o.status = 'DELIVERED'
    GROUP BY oi.book_id
) delivered
WHERE b.id = delivered.book_id;
