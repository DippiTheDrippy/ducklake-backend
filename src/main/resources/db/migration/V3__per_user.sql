CREATE TABLE favorites (
    user_id UUID NOT NULL,
    dataset_id UUID NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    PRIMARY KEY (user_id, dataset_id),

    CONSTRAINT fk_favorites_user
       FOREIGN KEY (user_id)
           REFERENCES users(id)
           ON DELETE CASCADE,

    CONSTRAINT fk_favorites_dataset
       FOREIGN KEY (dataset_id)
           REFERENCES datasets(id)
           ON DELETE CASCADE
);

CREATE TABLE credentials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    dataset_id UUID NOT NULL,
    user_id UUID NOT NULL,

    access_level TEXT NOT NULL,

    postgres_username TEXT NOT NULL,
    garage_access_key_id TEXT NOT NULL,

    expires_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_credentials_dataset
        FOREIGN KEY (dataset_id)
            REFERENCES datasets(id)
            ON DELETE CASCADE,

    CONSTRAINT fk_credentials_user
        FOREIGN KEY (user_id)
            REFERENCES users(id)
            ON DELETE CASCADE,

    CONSTRAINT chk_credentials_expiry
        CHECK (expires_at IS NULL OR expires_at > created_at)

    -- One credential per dataset per user
    CONSTRAINT uq_credentials_dataset_user
        UNIQUE (dataset_id, user_id)
);

-- favorites indexes
CREATE INDEX idx_favorites_user_id
    ON favorites(user_id);

CREATE INDEX idx_favorites_dataset_id
    ON favorites(dataset_id);

-- credentials indexes
CREATE INDEX idx_credentials_dataset_id
    ON credentials(dataset_id);

CREATE INDEX idx_credentials_user_id
    ON credentials(user_id);