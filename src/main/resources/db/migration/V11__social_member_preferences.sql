ALTER TABLE social_room_member
    ADD COLUMN selected_genre_ids INTEGER[] NOT NULL DEFAULT '{}',
    ADD COLUMN selected_vibe_id UUID REFERENCES vibe(id) ON DELETE SET NULL,
    ADD COLUMN ready BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN preference_updated_at TIMESTAMPTZ;

ALTER TABLE social_room_member
    ADD CONSTRAINT ck_social_member_genre_limit
        CHECK (cardinality(selected_genre_ids) <= 3),
    ADD CONSTRAINT ck_social_member_positive_genres
        CHECK (0 < ALL(selected_genre_ids)),
    ADD CONSTRAINT ck_social_member_ready_has_preference
        CHECK (
            ready = FALSE
            OR cardinality(selected_genre_ids) > 0
            OR selected_vibe_id IS NOT NULL
        );

CREATE INDEX idx_social_room_member_ready
    ON social_room_member (room_id, ready);
