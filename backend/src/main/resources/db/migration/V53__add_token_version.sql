-- Every JWT carries the account's token version; raising it revokes all earlier tokens.
-- Existing accounts start at 0.
ALTER TABLE users
    ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;
