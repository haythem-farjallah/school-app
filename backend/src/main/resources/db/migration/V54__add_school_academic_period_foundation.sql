CREATE TABLE schools (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE academic_years (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_academic_years_school FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE RESTRICT,
    CONSTRAINT uk_academic_years_school_name UNIQUE (school_id, name),
    CONSTRAINT ck_academic_years_dates CHECK (start_date < end_date)
);

CREATE UNIQUE INDEX uk_academic_years_active_school ON academic_years(school_id) WHERE active = TRUE;

CREATE TABLE terms (
    id BIGSERIAL PRIMARY KEY,
    academic_year_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    sequence_number INTEGER NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_terms_academic_year FOREIGN KEY (academic_year_id) REFERENCES academic_years(id) ON DELETE RESTRICT,
    CONSTRAINT uk_terms_year_name UNIQUE (academic_year_id, name),
    CONSTRAINT uk_terms_year_sequence UNIQUE (academic_year_id, sequence_number),
    CONSTRAINT ck_terms_dates CHECK (start_date < end_date),
    CONSTRAINT ck_terms_sequence_positive CHECK (sequence_number > 0)
);

-- The scoped unique constraints also index the parent keys for year and term lookups.
