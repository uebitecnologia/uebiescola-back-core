-- Ensures users.role CHECK constraint includes ROLE_PUBLISHER_ADMIN (marketplace).
-- In dev, Hibernate ddl-auto=update auto-adds the constraint from @Enumerated(EnumType.STRING);
-- in prod ddl-auto=validate does not, so we declare it explicitly here.

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;

ALTER TABLE users ADD CONSTRAINT users_role_check
    CHECK (role IN (
        'ROLE_CEO',
        'ROLE_ADMIN',
        'ROLE_TEACHER',
        'ROLE_GUARDIAN',
        'ROLE_PUBLISHER_ADMIN'
    ));
