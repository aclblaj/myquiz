-- =====================================================
-- MyQuiz Database Initialization Script
-- =====================================================
-- Purpose: Complete initial database setup for MyQuiz
-- Includes: Admin user, role/permission system
-- Run once: On first deployment or clean database setup
-- =====================================================

-- =====================================================
-- PART 1: Role and Permission Management System
-- =====================================================

-- Create users table (required by foreign keys)
CREATE TABLE IF NOT EXISTS users (
    user_id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) UNIQUE,
    hashed_password VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Create permissions table
CREATE TABLE IF NOT EXISTS permissions (
    permission_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    resource VARCHAR(50),
    action VARCHAR(10) NOT NULL CHECK (action IN ('READ', 'MODIFY'))
);

-- Create roles table
CREATE TABLE IF NOT EXISTS roles (
    role_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create user_roles junction table
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE
);

-- Create role_permissions junction table
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(permission_id) ON DELETE CASCADE
);

-- Add additional columns to users table if they don't exist (for compatibility)
-- These are already in the CREATE TABLE above, but kept for existing databases
-- ALTER TABLE users ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT true;
-- ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
-- ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

-- =====================================================
-- PART 2: Insert Default Permissions
-- =====================================================

INSERT INTO permissions (name, description, resource, action) VALUES
-- Course permissions
('READ_COURSE', 'View and list courses', 'COURSE', 'READ'),
('MODIFY_COURSE', 'Create, edit, delete courses', 'COURSE', 'MODIFY'),

-- Quiz permissions
('READ_QUIZ', 'View and list quizzes', 'QUIZ', 'READ'),
('MODIFY_QUIZ', 'Create, edit, delete quizzes', 'QUIZ', 'MODIFY'),

-- Question permissions
('READ_QUESTION', 'View and list questions', 'QUESTION', 'READ'),
('MODIFY_QUESTION', 'Create, edit, delete questions', 'QUESTION', 'MODIFY'),

-- Author permissions
('READ_AUTHOR', 'View and list authors', 'AUTHOR', 'READ'),
('MODIFY_AUTHOR', 'Create, edit, delete authors', 'AUTHOR', 'MODIFY'),

-- User management permissions
('READ_USER', 'View and list users', 'USER', 'READ'),
('MODIFY_USER', 'Create, edit, delete users', 'USER', 'MODIFY'),

-- Role management permissions
('READ_ROLE', 'View and list roles', 'ROLE', 'READ'),
('MODIFY_ROLE', 'Create, edit, delete roles', 'ROLE', 'MODIFY'),

-- Permission management
('MODIFY_PERMISSION', 'Assign permissions to roles', 'PERMISSION', 'MODIFY'),

-- AI Tools permission
('AI_TOOLS', 'Access AI correction tools', 'AI', 'MODIFY'),

-- Upload permission
('UPLOAD_FILES', 'Upload files and archives', 'UPLOAD', 'MODIFY'),

-- XML export permission
('EXPORT_XML', 'Export quiz/course questions as XML files', 'EXPORT', 'MODIFY'),

-- Statistics permission
('VIEW_EXTENDED_STATISTICS', 'View extended statistics and reports', 'STATISTICS', 'READ'),

-- Backup and restore permissions
('BACKUP_DATABASE', 'Export non-auth data as SQL backup', 'DATABASE', 'MODIFY'),
('RESTORE_DATABASE', 'Import non-auth data from SQL backup', 'DATABASE', 'MODIFY'),

-- Database cleanup permission
('CLEAN_DATABASE', 'Perform database cleanup operations', 'DATABASE', 'MODIFY'),

-- Duplicate detection permission
('VIEW_QUESTION_DUPLICATES', 'View and manage question duplicates', 'QUESTION', 'READ')

ON CONFLICT (name) DO NOTHING;

-- =====================================================
-- PART 3: Insert Default Roles
-- =====================================================

INSERT INTO roles (name, description) VALUES
('ADMINISTRATOR', 'Full system access with all permissions'),
('GUEST', 'Read-only access to view data'),
('TEACHER', 'Can create and manage courses, quizzes, and questions'),
('CONTENT_MANAGER', 'Can manage authors and upload content')
ON CONFLICT (name) DO NOTHING;

-- =====================================================
-- PART 4: Assign Permissions to Roles
-- =====================================================

-- Assign ALL permissions to ADMINISTRATOR
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMINISTRATOR'
ON CONFLICT DO NOTHING;

-- Assign READ permissions to GUEST
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'GUEST' AND p.action = 'READ'
ON CONFLICT DO NOTHING;

-- Assign permissions to TEACHER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'TEACHER' AND p.name IN (
    'READ_COURSE', 'MODIFY_COURSE',
    'READ_QUIZ', 'MODIFY_QUIZ',
    'READ_QUESTION', 'MODIFY_QUESTION',
    'READ_AUTHOR',
    'VIEW_QUESTION_DUPLICATES'
)
ON CONFLICT DO NOTHING;

-- Assign permissions to CONTENT_MANAGER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'CONTENT_MANAGER' AND p.name IN (
    'READ_AUTHOR', 'MODIFY_AUTHOR',
    'READ_COURSE',
    'READ_QUIZ',
    'READ_QUESTION',
    'UPLOAD_FILES',
    'AI_TOOLS',
    'VIEW_QUESTION_DUPLICATES'
)
ON CONFLICT DO NOTHING;

-- =====================================================
-- PART 5: Create Default Admin User
-- =====================================================
-- Clean-install source of truth: create default admin user here.
-- Username: admin
-- Password: admin
-- BCrypt hash generated for "admin":
-- $2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b3.
-- Idempotent insert keeps existing users untouched.

INSERT INTO users (username, email, hashed_password, enabled)
VALUES (
    'admin',
    'admin@myquiz.local',
    '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b3.',
    true
)
ON CONFLICT (username) DO NOTHING;

-- =====================================================
-- PART 6: Assign ADMINISTRATOR Role to Admin User (if exists)
-- =====================================================
-- This will assign the role if admin user exists from a previous initialization

INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u
CROSS JOIN roles r
WHERE u.username = 'admin' AND r.name = 'ADMINISTRATOR'
ON CONFLICT DO NOTHING;

-- =====================================================
-- PART 7: Create Performance Indexes
-- =====================================================
-- Note: Indexes for application tables (quiz, author, question, quiz_author) are
-- managed by JPA @Index annotations and created by Hibernate when the application starts

-- User and role management indexes
CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role_id ON user_roles(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_permission_id ON role_permissions(permission_id);
CREATE INDEX IF NOT EXISTS idx_permissions_resource ON permissions(resource);
CREATE INDEX IF NOT EXISTS idx_permissions_action ON permissions(action);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_enabled ON users(enabled);

-- Application table indexes (quiz, author, question, quiz_author) will be created
-- automatically by JPA when the application starts, using @Index annotations
--
-- INSERT INTO public.course
-- (id, course, created_at, description, semester, university_year, updated_at)
-- VALUES
-- -- Databases
-- (1, 'BD', '2026-04-14 14:40:00+03', 'Baze de date', 1, '2025-2026', '2026-04-14 14:40:00+03'),
-- (2, 'DB', '2026-04-14 14:40:00+03', 'Datenbanken', 1, '2025-2026', '2026-04-14 14:40:00+03'),
--
-- -- Computer Networks
-- (3, 'RC', '2026-04-14 14:40:00+03', 'Retele de calculatoare', 2, '2025-2026', '2026-04-14 14:40:00+03'),
-- (4, 'RN', '2026-04-14 14:40:00+03', 'Rechnernetze', 2, '2025-2026', '2026-04-14 14:40:00+03'),
--
-- -- Operating Systems
-- (5, 'SO', '2026-04-14 14:40:00+03', 'Sisteme de operare', 1, '2025-2026', '2026-04-14 14:40:00+03'),
-- (6, 'BS', '2026-04-14 14:40:00+03', 'Betriebssysteme', 1, '2025-2026', '2026-04-14 14:40:00+03'),
--
-- -- Distributed Databases
-- (7, 'BDD', '2026-04-14 14:40:00+03', 'Baze de date distribuite', 2, '2025-2026', '2026-04-14 14:40:00+03'),
-- (8, 'VDB', '2026-04-14 14:40:00+03', 'Verteilte Datenbanken', 2, '2025-2026', '2026-04-14 14:40:00+03'),
--
-- -- Network Algorithms
-- (9, 'NETZALG', '2026-04-14 14:40:00+03', 'Network Algorithms', 1, '2025-2026', '2026-04-14 14:40:00+03'),
--
-- -- IT Security / Cybersecurity
-- (10, 'ITSec', '2026-04-14 14:40:00+03', 'Securitatea sistemelor informatice', 2, '2025-2026', '2026-04-14 14:40:00+03'),
-- (11, 'ItSich', '2026-04-14 14:40:00+03', 'IT-Sicherheit', 2, '2025-2026', '2026-04-14 14:40:00+03'),
-- (12, 'Cyber',  '2026-04-14 14:40:00+03', 'Cybersecurity', 2, '2025-2026', '2026-04-14 14:40:00+03');
--
-- SELECT setval('public.course_seq', 12, true);

-- =====================================================
-- PART 8: Verification Summary
-- =====================================================

DO $$
DECLARE
    perm_count INT;
    role_count INT;
    admin_perm_count INT;
    admin_role TEXT;
BEGIN
    SELECT COUNT(*) INTO perm_count FROM permissions;
    SELECT COUNT(*) INTO role_count FROM roles;
    SELECT COUNT(*) INTO admin_perm_count FROM role_permissions rp
        JOIN roles r ON rp.role_id = r.role_id
        WHERE r.name = 'ADMINISTRATOR';
    SELECT r.name INTO admin_role FROM user_roles ur
        JOIN users u ON ur.user_id = u.user_id
        JOIN roles r ON ur.role_id = r.role_id
        WHERE u.username = 'admin';

    RAISE NOTICE '========================================';
    RAISE NOTICE 'MyQuiz Database Initialization Complete';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Permissions created: %', perm_count;
    RAISE NOTICE 'Roles created: %', role_count;
    RAISE NOTICE 'ADMINISTRATOR permissions: %', admin_perm_count;
    RAISE NOTICE 'Admin user role: %', admin_role;
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Default Login Credentials:';
    RAISE NOTICE '  Username: admin';
    RAISE NOTICE '  Password: admin';
    RAISE NOTICE '========================================';
END $$;

-- =====================================================
-- PART 9: Ensure question_bank_study_year_check Constraint Is Current
-- =====================================================
-- Hibernate auto-generates this constraint from the StudyYear enum.
-- When new enum values are added to Java, the DB constraint must be
-- updated manually because ddl-auto: validate does not modify it.
-- This block is idempotent and safe to run on existing databases.

ALTER TABLE IF EXISTS question_bank DROP CONSTRAINT IF EXISTS question_bank_study_year_check;

ALTER TABLE IF EXISTS question_bank ADD CONSTRAINT question_bank_study_year_check
    CHECK (study_year IN (
        'Y2020_2021',
        'Y2021_2022',
        'Y2022_2023',
        'Y2024_2025',
        'Y2025_2026',
        'Y2026_2027'
    ));

-- =====================================================
-- Initialization Complete
-- =====================================================
