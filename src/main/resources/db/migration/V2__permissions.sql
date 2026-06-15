CREATE TABLE dataset_user_permissions (
    dataset_id UUID NOT NULL,
    user_id UUID NOT NULL,

    access_level TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    PRIMARY KEY (dataset_id, user_id),

    CONSTRAINT fk_dataset_user_permissions_dataset
        FOREIGN KEY (dataset_id)
            REFERENCES datasets(id)
            ON DELETE CASCADE,

    CONSTRAINT fk_dataset_user_permissions_user
        FOREIGN KEY (user_id)
            REFERENCES users(id)
            ON DELETE CASCADE,

    CONSTRAINT chk_dataset_user_permissions_access_level
        CHECK (access_level IN ('READ', 'WRITE'))
);

CREATE TABLE dataset_group_permissions (
    dataset_id UUID NOT NULL,
    group_id UUID NOT NULL,

    access_level TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    PRIMARY KEY (dataset_id, group_id),

    CONSTRAINT fk_dataset_group_permissions_dataset
       FOREIGN KEY (dataset_id)
           REFERENCES datasets(id)
           ON DELETE CASCADE,

    CONSTRAINT fk_dataset_group_permissions_group
       FOREIGN KEY (group_id)
           REFERENCES user_groups(id)
           ON DELETE CASCADE,

    CONSTRAINT chk_dataset_group_permissions_access_level
       CHECK (access_level IN ('READ', 'WRITE'))
);

-- permissions indexes
CREATE INDEX idx_dataset_user_permissions_user_id
    ON dataset_user_permissions(user_id);

CREATE INDEX idx_dataset_group_permissions_group_id
    ON dataset_group_permissions(group_id);