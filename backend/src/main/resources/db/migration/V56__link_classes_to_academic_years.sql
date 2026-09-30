-- Historical classes stay unlinked until their ownership can be reconciled explicitly.
-- The compatibility projection must hold every valid canonical AcademicYear name.
ALTER TABLE classes ALTER COLUMN academic_year TYPE VARCHAR(255);

ALTER TABLE classes ADD COLUMN academic_year_id BIGINT NULL;

ALTER TABLE classes ADD CONSTRAINT fk_classes_academic_year
    FOREIGN KEY (academic_year_id) REFERENCES academic_years(id) ON DELETE RESTRICT;

CREATE INDEX idx_classes_academic_year ON classes(academic_year_id);
