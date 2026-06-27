INSERT INTO users (email, password_hash, full_name, role, enabled, email_verified, email_verified_at, created_at, updated_at)
VALUES ('customer@bookverse.local', '$2a$10$O40rJV7wpiSFdRVGxEZSmey6GvpdulHkEI/yqI5k5zDoghoNgDcta', 'BookVerse Customer', 'CUSTOMER', true, true, now(), now(), now())
ON CONFLICT (email) DO NOTHING; -- ChangeMe123!

INSERT INTO users (email, password_hash, full_name, role, enabled, email_verified, email_verified_at, created_at, updated_at)
VALUES ('customer2@gmail.com', '$2a$10$O40rJV7wpiSFdRVGxEZSmey6GvpdulHkEI/yqI5k5zDoghoNgDcta', 'Customer2', 'CUSTOMER', true, true, now(), now(), now())
ON CONFLICT (email) DO NOTHING; -- ChangeMe123!
