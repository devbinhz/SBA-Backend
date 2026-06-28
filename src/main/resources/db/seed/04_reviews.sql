INSERT INTO reviews (book_id, user_id, rating, comment, created_at, updated_at)
VALUES (
    (SELECT id FROM books WHERE isbn = '9781805127857'),
    (SELECT id FROM users WHERE email = 'customer@bookverse.local'),
    5,
    'Sách rất hay, hướng dẫn chi tiết về FastAPI!',
    now(),
    now()
)
ON CONFLICT (book_id, user_id) DO NOTHING;

INSERT INTO reviews (book_id, user_id, rating, comment, created_at, updated_at)
VALUES (
    (SELECT id FROM books WHERE isbn = '9781098166304'),
    (SELECT id FROM users WHERE email = 'customer@bookverse.local'),
    5,
    'Tài liệu rất chất lượng cho kỹ sư AI làm việc với LLMs.',
    now(),
    now()
)
ON CONFLICT (book_id, user_id) DO NOTHING;

INSERT INTO reviews (book_id, user_id, rating, comment, created_at, updated_at)
VALUES (
    (SELECT id FROM books WHERE isbn = '9781626251700'),
    (SELECT id FROM users WHERE email = 'customer@bookverse.local'),
    4,
    'Quyển sách tâm lý học rất sâu sắc, giúp chữa lành những tổn thương thơ ấu.',
    now(),
    now()
)
ON CONFLICT (book_id, user_id) DO NOTHING;

INSERT INTO reviews (book_id, user_id, rating, comment, created_at, updated_at)
VALUES (
    (SELECT id FROM books WHERE isbn = '9781883629007'),
    (SELECT id FROM users WHERE email = 'customer@bookverse.local'),
    4,
    'Rất nhiều phương pháp tư duy giải quyết vấn đề sáng tạo trong kinh doanh.',
    now(),
    now()
)
ON CONFLICT (book_id, user_id) DO NOTHING;

INSERT INTO reviews (book_id, user_id, rating, comment, created_at, updated_at)
VALUES (
    (SELECT id FROM books WHERE isbn = '9780470187715'),
    (SELECT id FROM users WHERE email = 'customer@bookverse.local'),
    3,
    'Tạm ổn, phù hợp với các trader mới bắt đầu tìm hiểu quản lý vốn.',
    now(),
    now()
)
ON CONFLICT (book_id, user_id) DO NOTHING;

INSERT INTO reviews (book_id, user_id, rating, comment, created_at, updated_at)
VALUES (
    (SELECT id FROM books WHERE isbn = '9781805127857'),
    (SELECT id FROM users WHERE email = 'customer2@gmail.com'),
    4,
    'Nội dung cookbook áp dụng trực tiếp được luôn vào dự án thực tế.',
    now(),
    now()
)
ON CONFLICT (book_id, user_id) DO NOTHING;

INSERT INTO reviews (book_id, user_id, rating, comment, created_at, updated_at)
VALUES (
    (SELECT id FROM books WHERE isbn = '9781098166304'),
    (SELECT id FROM users WHERE email = 'customer2@gmail.com'),
    4,
    'Rất thích phần hướng dẫn tối ưu hóa suy luận của tác giả Chip Huyen.',
    now(),
    now()
)
ON CONFLICT (book_id, user_id) DO NOTHING;

INSERT INTO reviews (book_id, user_id, rating, comment, created_at, updated_at)
VALUES (
    (SELECT id FROM books WHERE isbn = '9781098107635'),
    (SELECT id FROM users WHERE email = 'customer2@gmail.com'),
    5,
    'Must-read cho ai muốn hiểu bản chất toán học sau các thuật toán AI.',
    now(),
    now()
)
ON CONFLICT (book_id, user_id) DO NOTHING;

INSERT INTO reviews (book_id, user_id, rating, comment, created_at, updated_at)
VALUES (
    (SELECT id FROM books WHERE isbn = '9781626251700'),
    (SELECT id FROM users WHERE email = 'customer2@gmail.com'),
    5,
    'Tuyệt vời, giúp tôi hiểu thêm về hành vi của cha mẹ và giải tỏa bản thân.',
    now(),
    now()
)
ON CONFLICT (book_id, user_id) DO NOTHING;

INSERT INTO reviews (book_id, user_id, rating, comment, created_at, updated_at)
VALUES (
    (SELECT id FROM books WHERE isbn = '9780300273601'),
    (SELECT id FROM users WHERE email = 'customer2@gmail.com'),
    4,
    'Góc nhìn khách quan về giáo dục đại học tinh hoa của Mỹ.',
    now(),
    now()
)
ON CONFLICT (book_id, user_id) DO NOTHING;
