ALTER TABLE users
    ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'STUDENT';

-- once every user has an explicit role, you can still drop the default:
ALTER TABLE users
    ALTER COLUMN role DROP DEFAULT;