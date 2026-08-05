CREATE TABLE social_room (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    room_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    invite_code VARCHAR(8) NOT NULL,
    spin_sequence BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    closed_at TIMESTAMPTZ,

    CONSTRAINT uk_social_room_invite_code UNIQUE (invite_code),
    CONSTRAINT ck_social_room_type CHECK (room_type IN ('COUPLE', 'GROUP')),
    CONSTRAINT ck_social_room_status CHECK (status IN ('OPEN', 'CLOSED')),
    CONSTRAINT ck_social_room_closed_at CHECK (
        (status = 'OPEN' AND closed_at IS NULL)
        OR (status = 'CLOSED' AND closed_at IS NOT NULL)
    )
);

CREATE TABLE social_room_member (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL REFERENCES social_room(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    member_role VARCHAR(20) NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT uk_social_room_member UNIQUE (room_id, user_id),
    CONSTRAINT ck_social_room_member_role CHECK (member_role IN ('HOST', 'MEMBER'))
);

CREATE TABLE social_room_spin (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL REFERENCES social_room(id) ON DELETE CASCADE,
    triggered_by_user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    idempotency_key VARCHAR(100) NOT NULL,
    spin_number BIGINT NOT NULL,
    filters JSONB NOT NULL,
    movie_result JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT uk_social_room_spin_idempotency UNIQUE (room_id, idempotency_key),
    CONSTRAINT uk_social_room_spin_number UNIQUE (room_id, spin_number)
);

CREATE INDEX idx_social_room_owner_status
    ON social_room (owner_user_id, status, updated_at DESC);

CREATE INDEX idx_social_room_member_user
    ON social_room_member (user_id, room_id);

CREATE INDEX idx_social_room_spin_latest
    ON social_room_spin (room_id, spin_number DESC);
