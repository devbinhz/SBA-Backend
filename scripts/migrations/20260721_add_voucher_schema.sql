-- Add campaigns table
CREATE TABLE IF NOT EXISTS campaigns (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    campaign_type VARCHAR(50) NOT NULL CHECK (campaign_type IN ('FLASH_SALE', 'WELCOME_GIFT')),
    is_auto_distributed BOOLEAN NOT NULL DEFAULT FALSE,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'COMPLETED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Recreate vouchers table
DROP TABLE IF EXISTS user_vouchers;
DROP TABLE IF EXISTS vouchers;

CREATE TABLE vouchers (
    id BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT REFERENCES campaigns(id),
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    discount_type VARCHAR(20) NOT NULL CHECK (discount_type IN ('FIXED_AMOUNT', 'PERCENTAGE')),
    discount_value BIGINT NOT NULL CHECK (discount_value > 0),
    max_discount_amount BIGINT,
    min_order_value BIGINT NOT NULL DEFAULT 0 CHECK (min_order_value >= 0),
    total_quantity INT NOT NULL CHECK (total_quantity > 0),
    claimed_quantity INT NOT NULL DEFAULT 0 CHECK (claimed_quantity >= 0 AND claimed_quantity <= total_quantity),
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'EXPIRED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_vouchers_campaign ON vouchers(campaign_id);

-- Recreate user_vouchers table
CREATE TABLE user_vouchers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    voucher_id BIGINT NOT NULL REFERENCES vouchers(id),
    status VARCHAR(20) NOT NULL DEFAULT 'UNUSED' CHECK (status IN ('UNUSED', 'USED', 'EXPIRED')),
    claimed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, voucher_id)
);

CREATE INDEX idx_user_vouchers_expires ON user_vouchers(expires_at) WHERE status = 'UNUSED';
CREATE INDEX idx_user_vouchers_user ON user_vouchers(user_id, status);
