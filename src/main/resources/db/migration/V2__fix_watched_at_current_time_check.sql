CREATE OR REPLACE FUNCTION reject_future_watched_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    -- CURRENT_TIMESTAMP is fixed at transaction start. clock_timestamp() is the
    -- actual wall-clock time and accepts watched_at generated later in the same transaction.
    IF NEW.watched_at IS NOT NULL AND NEW.watched_at > clock_timestamp() THEN
        RAISE EXCEPTION 'watched_at cannot be in the future'
            USING ERRCODE = '23514', CONSTRAINT = 'ck_user_movie_history_watched_at_not_future';
    END IF;
    RETURN NEW;
END;
$$;
