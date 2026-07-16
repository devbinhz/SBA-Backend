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
    (SELECT id FROM books WHERE isbn = '9781883629007'),
    (SELECT id FROM users WHERE email = 'customer2@gmail.com'),
    5,
    'A practical collection of techniques that works well for team workshops.',
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

-- Keep one hidden review and its audit entry for deterministic moderation demos.
UPDATE reviews r
SET status = 'HIDDEN',
    moderation_reason = 'Hidden demo review for moderation workflow',
    moderated_by = (SELECT id FROM users WHERE email = 'admin@bookverse.local'),
    moderated_at = now() - interval '1 day',
    updated_at = now() - interval '1 day'
WHERE r.book_id = (SELECT id FROM books WHERE isbn = '9781805127857')
  AND r.user_id = (SELECT id FROM users WHERE email = 'customer@bookverse.local');

INSERT INTO review_moderation_history
    (review_id, from_status, to_status, reason, moderated_by, moderator_name, created_at)
SELECT r.id, 'PUBLISHED', 'HIDDEN', 'Hidden demo review for moderation workflow',
       a.id, a.full_name, now() - interval '1 day'
FROM reviews r
JOIN users a ON a.email = 'admin@bookverse.local'
WHERE r.book_id = (SELECT id FROM books WHERE isbn = '9781805127857')
  AND r.user_id = (SELECT id FROM users WHERE email = 'customer@bookverse.local');

-- Keep catalog aggregates derived from published reviews instead of hard-coded book values.
UPDATE books b
SET rating_avg = review_stats.average_rating,
    review_count = review_stats.review_count,
    updated_at = now()
FROM (
    SELECT book_id,
           ROUND(AVG(rating)::numeric, 2) AS average_rating,
           COUNT(*)::integer AS review_count
    FROM reviews
    WHERE status = 'PUBLISHED'
    GROUP BY book_id
) review_stats
WHERE b.id = review_stats.book_id;

UPDATE books b
SET rating_avg = 0,
    review_count = 0,
    updated_at = now()
WHERE NOT EXISTS (
    SELECT 1
    FROM reviews r
    WHERE r.book_id = b.id
      AND r.status = 'PUBLISHED'
);
