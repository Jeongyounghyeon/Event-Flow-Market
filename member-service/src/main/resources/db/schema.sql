CREATE TABLE IF NOT EXISTS members
(
    id             BIGSERIAL PRIMARY KEY,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password       VARCHAR(255),                            -- OAuth 전용 계정은 null
    nickname       VARCHAR(100) NOT NULL,
    role           VARCHAR(20)  NOT NULL DEFAULT 'USER',    -- USER | ADMIN
    oauth_provider VARCHAR(20),                             -- KAKAO | NAVER | null (자체 로그인)
    oauth_id       VARCHAR(255),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_members_email ON members (email);
CREATE INDEX IF NOT EXISTS idx_members_oauth ON members (oauth_provider, oauth_id);
