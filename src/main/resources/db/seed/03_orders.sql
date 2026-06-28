INSERT INTO orders (user_id, status, subtotal, shipping_fee, total, address_snapshot, payment_method, idempotency_key, created_at, updated_at, paid_at)
VALUES (
    (SELECT id FROM users WHERE email = 'customer@bookverse.local'),
    'PAID',
    390000,
    30000,
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

INSERT INTO orders (user_id, status, subtotal, shipping_fee, total, address_snapshot, payment_method, idempotency_key, created_at, updated_at, paid_at, delivered_at)
VALUES (
    (SELECT id FROM users WHERE email = 'customer@bookverse.local'),
    'DELIVERED',
    405000,
    30000,
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
