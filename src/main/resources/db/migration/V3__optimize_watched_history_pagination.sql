CREATE INDEX idx_user_movie_history_watched_collection
    ON user_movie_history (user_id, watched_at DESC NULLS LAST, id DESC)
    WHERE status = 'WATCHED';
