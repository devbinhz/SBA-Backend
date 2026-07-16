INSERT INTO users (email, password_hash, full_name, role, enabled, email_verified, email_verified_at, created_at, updated_at)
VALUES ('customer@bookverse.local', '$2a$10$O40rJV7wpiSFdRVGxEZSmey6GvpdulHkEI/yqI5k5zDoghoNgDcta', 'BookVerse Customer', 'CUSTOMER', true, true, now(), now(), now())
ON CONFLICT (email) DO NOTHING; -- ChangeMe123!

INSERT INTO users (email, password_hash, full_name, role, enabled, email_verified, email_verified_at, created_at, updated_at)
VALUES ('customer2@gmail.com', '$2a$10$O40rJV7wpiSFdRVGxEZSmey6GvpdulHkEI/yqI5k5zDoghoNgDcta', 'Customer2', 'CUSTOMER', true, true, now(), now(), now())
ON CONFLICT (email) DO NOTHING; -- ChangeMe123!

-- Reset-only demo admin. Normal/production startup still uses AdminSeeder environment variables.
INSERT INTO users (email, password_hash, full_name, role, enabled, email_verified, email_verified_at, created_at, updated_at)
VALUES ('admin@bookverse.local', '$2a$10$O40rJV7wpiSFdRVGxEZSmey6GvpdulHkEI/yqI5k5zDoghoNgDcta', 'BookVerse Admin', 'ADMIN', true, true, now(), now(), now())
ON CONFLICT (email) DO NOTHING; -- ChangeMe123!

-- A disabled account keeps the admin user-status screen deterministic.
INSERT INTO users (email, password_hash, full_name, role, enabled, email_verified, email_verified_at, created_at, updated_at)
VALUES ('locked.customer@bookverse.local', '$2a$10$O40rJV7wpiSFdRVGxEZSmey6GvpdulHkEI/yqI5k5zDoghoNgDcta', 'Locked Demo Customer', 'CUSTOMER', false, true, now(), now(), now())
ON CONFLICT (email) DO NOTHING; -- ChangeMe123!

INSERT INTO addresses (user_id, recipient, phone, line, ward, district, city, is_default, created_at, updated_at)
SELECT id, 'BookVerse Customer', '0987654321', '123 Main Street', 'Ward 1', 'Ba Dinh', 'Hanoi', true, now(), now()
FROM users
WHERE email = 'customer@bookverse.local';

INSERT INTO addresses (user_id, recipient, phone, line, ward, district, city, is_default, created_at, updated_at)
SELECT id, 'Customer2', '0900000000', '456 Demo Street', 'Ben Nghe Ward', 'District 1', 'Ho Chi Minh City', true, now(), now()
FROM users
WHERE email = 'customer2@gmail.com';
