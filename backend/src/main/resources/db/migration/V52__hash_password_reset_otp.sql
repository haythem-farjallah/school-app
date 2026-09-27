-- Password-reset codes are stored as a one-way hash, and each issued code allows
-- a limited number of wrong attempts. Codes still stored in plaintext are discarded:
-- their holders request a new one.
ALTER TABLE users
    ALTER COLUMN otp_code TYPE VARCHAR(255),
    ADD COLUMN otp_failed_attempts INTEGER NOT NULL DEFAULT 0;

UPDATE users
SET otp_code = NULL,
    otp_expiry = NULL
WHERE otp_code IS NOT NULL;
