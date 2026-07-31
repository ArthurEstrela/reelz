-- Users created before the onboarding flow was introduced must keep direct
-- access to the product. New users are inserted after this migration and keep
-- onboarding_completed_at NULL until they finish the movie selection.
UPDATE user_account
   SET onboarding_completed_at = CURRENT_TIMESTAMP
 WHERE onboarding_completed_at IS NULL;
