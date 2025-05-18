/* ----------------------------------------------------------------------
   ENUM  (creates only if missing)
------------------------------------------------------------------------ */
DO $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_status') THEN
            CREATE TYPE user_status AS ENUM ('ACTIVE','SUSPENDED','DELETED');
        END IF;
    END $$;

/* ----------------------------------------------------------------------
   MAIN ALTER – all new BaseUser columns
------------------------------------------------------------------------ */
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS version      BIGINT      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS created_at   TIMESTAMP   NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS updated_at   TIMESTAMP   NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS status       user_status NOT NULL DEFAULT 'ACTIVE';

/* ----------------------------------------------------------------------
   UNIQUE INDEX on email (safe if already present)
------------------------------------------------------------------------ */
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON users(email);

/* ----------------------------------------------------------------------
   SIMPLE updated_at touch trigger
------------------------------------------------------------------------ */
CREATE OR REPLACE FUNCTION trg_users_touch_updated_at()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS users_touch_updated_at ON users;

CREATE TRIGGER users_touch_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION trg_users_touch_updated_at();
