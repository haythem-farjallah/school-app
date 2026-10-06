-- ResourceComment now references learning_resources through V64. Drop only the
-- obsolete Resource aggregate; unexpected dependencies must fail the migration.
DROP TABLE resource_allowed_roles;
DROP TABLE resources;
