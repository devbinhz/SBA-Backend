BEGIN;

-- Heal denormalized book rating fields after seed/manual review inserts.
UPDATE books b
SET rating_avg = review_stats.average_rating,
    review_count = review_stats.review_count,
    updated_at = now()
FROM (
    SELECT book_id,
           ROUND(AVG(rating)::numeric, 1) AS average_rating,
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

COMMIT;
