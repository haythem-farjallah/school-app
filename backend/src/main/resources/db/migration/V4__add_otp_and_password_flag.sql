-- add OTP columns and first-login flag to BaseUser
ALTER TABLE users
    ADD COLUMN otp_code           VARCHAR(6),
  ADD COLUMN otp_expiry         TIMESTAMP,
  ADD COLUMN password_change_required BOOLEAN NOT NULL DEFAULT FALSE;
