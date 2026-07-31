CREATE INDEX idx_user_movie_history_watchlist_collection
    ON user_movie_history (user_id, updated_at DESC, id DESC)
    WHERE status = 'WATCHLIST';
