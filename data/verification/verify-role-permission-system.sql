-- =====================================================
-- Role and Permission System - Verification Queries
-- =====================================================
-- Run these queries after migration to verify the system
-- is correctly set up.
-- =====================================================

\echo ''
\echo '=========================================='
\echo 'ROLE AND PERMISSION SYSTEM VERIFICATION'
\echo '=========================================='
\echo ''

-- =====================================================
-- 1. Check Tables Created
-- =====================================================
\echo '1. Checking tables...'
\echo ''

SELECT
    table_name,
    CASE
        WHEN table_name IN ('users', 'roles', 'permissions', 'user_roles', 'role_permissions')
        THEN '✅'
        ELSE '❌'
    END as status
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name IN ('users', 'roles', 'permissions', 'user_roles', 'role_permissions')
ORDER BY table_name;

\echo ''

-- =====================================================
-- 2. Count Records in Each Table
-- =====================================================
\echo '2. Counting records in each table...'
\echo ''

SELECT 'users' as table_name, COUNT(*) as record_count FROM users
UNION ALL
SELECT 'roles', COUNT(*) FROM roles
UNION ALL
SELECT 'permissions', COUNT(*) FROM permissions
UNION ALL
SELECT 'user_roles', COUNT(*) FROM user_roles
UNION ALL
SELECT 'role_permissions', COUNT(*) FROM role_permissions
ORDER BY table_name;

\echo ''

-- =====================================================
-- 3. List All Permissions by Resource
-- =====================================================
\echo '3. All Permissions grouped by Resource:'
\echo ''

SELECT
    resource,
    action,
    name,
    description
FROM permissions
ORDER BY resource, action, name;

\echo ''

-- =====================================================
-- 4. List All Roles with Permission Count
-- =====================================================
\echo '4. All Roles with Permission Count:'
\echo ''

SELECT
    r.role_id,
    r.name as role_name,
    r.description,
    COUNT(rp.permission_id) as permission_count
FROM roles r
LEFT JOIN role_permissions rp ON r.role_id = rp.role_id
GROUP BY r.role_id, r.name, r.description
ORDER BY r.name;

\echo ''

-- =====================================================
-- 5. ADMINISTRATOR Role - Should Have All Permissions
-- =====================================================
\echo '5. ADMINISTRATOR Role Permissions:'
\echo ''

SELECT
    p.name as permission_name,
    p.resource,
    p.action
FROM role_permissions rp
JOIN roles r ON rp.role_id = r.role_id
JOIN permissions p ON rp.permission_id = p.permission_id
WHERE r.name = 'ADMINISTRATOR'
ORDER BY p.resource, p.action;

\echo ''
\echo 'Expected: all permissions in system (dynamic count)'
SELECT 'Total: ' || COUNT(*) || ' permissions' as summary
FROM role_permissions rp
JOIN roles r ON rp.role_id = r.role_id
WHERE r.name = 'ADMINISTRATOR';

\echo ''

-- =====================================================
-- 6. GUEST Role - Should Have Only READ Permissions
-- =====================================================
\echo '6. GUEST Role Permissions:'
\echo ''

SELECT
    p.name as permission_name,
    p.resource,
    p.action
FROM role_permissions rp
JOIN roles r ON rp.role_id = r.role_id
JOIN permissions p ON rp.permission_id = p.permission_id
WHERE r.name = 'GUEST'
ORDER BY p.resource;

\echo ''
\echo 'Expected: all READ permissions in system (dynamic count)'
SELECT 'Total: ' || COUNT(*) || ' permissions' as summary
FROM role_permissions rp
JOIN roles r ON rp.role_id = r.role_id
JOIN permissions p ON rp.permission_id = p.permission_id
WHERE r.name = 'GUEST' AND p.action = 'READ';

\echo ''

-- =====================================================
-- 7. TEACHER Role Permissions
-- =====================================================
\echo '7. TEACHER Role Permissions:'
\echo ''

SELECT
    p.name as permission_name,
    p.resource,
    p.action
FROM role_permissions rp
JOIN roles r ON rp.role_id = r.role_id
JOIN permissions p ON rp.permission_id = p.permission_id
WHERE r.name = 'TEACHER'
ORDER BY p.resource, p.action;

\echo ''

-- =====================================================
-- 8. CONTENT_MANAGER Role Permissions
-- =====================================================
\echo '8. CONTENT_MANAGER Role Permissions:'
\echo ''

SELECT
    p.name as permission_name,
    p.resource,
    p.action
FROM role_permissions rp
JOIN roles r ON rp.role_id = r.role_id
JOIN permissions p ON rp.permission_id = p.permission_id
WHERE r.name = 'CONTENT_MANAGER'
ORDER BY p.resource, p.action;

\echo ''

-- =====================================================
-- 9. Admin User - Should Have ADMINISTRATOR Role
-- =====================================================
\echo '9. Admin User Verification:'
\echo ''

SELECT
    u.user_id,
    u.username,
    u.email,
    u.enabled,
    r.name as role_name,
    ur.assigned_at
FROM users u
LEFT JOIN user_roles ur ON u.user_id = ur.user_id
LEFT JOIN roles r ON ur.role_id = r.role_id
WHERE u.username = 'admin';

\echo ''
\echo 'Expected: admin user with ADMINISTRATOR role'
\echo ''

-- =====================================================
-- 10. Admin User Effective Permissions
-- =====================================================
\echo '10. Admin User Effective Permissions (from all roles):'
\echo ''

SELECT DISTINCT
    p.name as permission_name,
    p.resource,
    p.action,
    p.description
FROM users u
JOIN user_roles ur ON u.user_id = ur.user_id
JOIN roles r ON ur.role_id = r.role_id
JOIN role_permissions rp ON r.role_id = rp.role_id
JOIN permissions p ON rp.permission_id = p.permission_id
WHERE u.username = 'admin'
ORDER BY p.resource, p.action;

\echo ''
SELECT 'Total permissions for admin: ' || COUNT(DISTINCT p.permission_id) as summary
FROM users u
JOIN user_roles ur ON u.user_id = ur.user_id
JOIN roles r ON ur.role_id = r.role_id
JOIN role_permissions rp ON r.role_id = rp.role_id
JOIN permissions p ON rp.permission_id = p.permission_id
WHERE u.username = 'admin';

\echo ''

-- =====================================================
-- 11. Check for Users Without Roles
-- =====================================================
\echo '11. Users Without Roles (should be empty or only test users):'
\echo ''

SELECT
    u.user_id,
    u.username,
    u.email,
    u.enabled
FROM users u
LEFT JOIN user_roles ur ON u.user_id = ur.user_id
WHERE ur.role_id IS NULL;

\echo ''

-- =====================================================
-- 12. Check Indexes Created
-- =====================================================
\echo '12. Checking Indexes:'
\echo ''

SELECT
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'public'
  AND tablename IN ('user_roles', 'role_permissions', 'permissions', 'users')
ORDER BY tablename, indexname;

\echo ''

-- =====================================================
-- 13. Permission Matrix by Role
-- =====================================================
\echo '13. Permission Matrix by Role:'
\echo ''

SELECT
    r.name as role,
    p.resource,
    STRING_AGG(p.action::text, ', ' ORDER BY p.action) as actions
FROM roles r
JOIN role_permissions rp ON r.role_id = rp.role_id
JOIN permissions p ON rp.permission_id = p.permission_id
GROUP BY r.name, p.resource
ORDER BY r.name, p.resource;

\echo ''

-- =====================================================
-- 14. Roles with No Permissions (should be empty)
-- =====================================================
\echo '14. Roles with No Permissions (should be empty):'
\echo ''

SELECT
    r.role_id,
    r.name as role_name,
    r.description
FROM roles r
LEFT JOIN role_permissions rp ON r.role_id = rp.role_id
WHERE rp.permission_id IS NULL;

\echo ''

-- =====================================================
-- 15. Summary Statistics
-- =====================================================
\echo '15. Summary Statistics:'
\echo ''

SELECT
    'Total Users' as metric,
    COUNT(*)::text as value
FROM users
UNION ALL
SELECT
    'Active Users',
    COUNT(*)::text
FROM users
WHERE enabled = true
UNION ALL
SELECT
    'Total Roles',
    COUNT(*)::text
FROM roles
UNION ALL
SELECT
    'Total Permissions',
    COUNT(*)::text
FROM permissions
UNION ALL
SELECT
    'READ Permissions',
    COUNT(*)::text
FROM permissions
WHERE action = 'READ'
UNION ALL
SELECT
    'MODIFY Permissions',
    COUNT(*)::text
FROM permissions
WHERE action = 'MODIFY'
UNION ALL
SELECT
    'Total Role Assignments',
    COUNT(*)::text
FROM user_roles
UNION ALL
SELECT
    'Total Permission Assignments',
    COUNT(*)::text
FROM role_permissions;

\echo ''
\echo '=========================================='
\echo 'VERIFICATION COMPLETE'
\echo '=========================================='
\echo ''

-- =====================================================
-- 16. Validation Checks
-- =====================================================
\echo '16. Validation Checks:'
\echo ''

DO $$
DECLARE
    admin_count INTEGER;
    admin_role_count INTEGER;
    admin_permission_count INTEGER;
    guest_role_exists BOOLEAN;
    total_permission_count INTEGER;
    all_checks_passed BOOLEAN := true;
BEGIN
    -- Check 1: Admin user exists
    SELECT COUNT(*) INTO admin_count FROM users WHERE username = 'admin';
    SELECT COUNT(*) INTO total_permission_count FROM permissions;
    IF admin_count = 0 THEN
        RAISE NOTICE '❌ FAIL: Admin user not found';
        all_checks_passed := false;
    ELSE
        RAISE NOTICE '✅ PASS: Admin user exists';
    END IF;

    -- Check 2: Admin has ADMINISTRATOR role
    SELECT COUNT(*) INTO admin_role_count
    FROM users u
    JOIN user_roles ur ON u.user_id = ur.user_id
    JOIN roles r ON ur.role_id = r.role_id
    WHERE u.username = 'admin' AND r.name = 'ADMINISTRATOR';

    IF admin_role_count = 0 THEN
        RAISE NOTICE '❌ FAIL: Admin user does not have ADMINISTRATOR role';
        all_checks_passed := false;
    ELSE
        RAISE NOTICE '✅ PASS: Admin user has ADMINISTRATOR role';
    END IF;

    -- Check 3: Admin has all permissions
    SELECT COUNT(DISTINCT p.permission_id) INTO admin_permission_count
    FROM users u
    JOIN user_roles ur ON u.user_id = ur.user_id
    JOIN roles r ON ur.role_id = r.role_id
    JOIN role_permissions rp ON r.role_id = rp.role_id
    JOIN permissions p ON rp.permission_id = p.permission_id
    WHERE u.username = 'admin';

    IF admin_permission_count < total_permission_count THEN
        RAISE NOTICE '❌ FAIL: Admin user has only % permissions (expected %)', admin_permission_count, total_permission_count;
        all_checks_passed := false;
    ELSE
        RAISE NOTICE '✅ PASS: Admin user has all % permissions', total_permission_count;
    END IF;

    -- Check 4: GUEST role exists
    SELECT EXISTS(SELECT 1 FROM roles WHERE name = 'GUEST') INTO guest_role_exists;
    IF NOT guest_role_exists THEN
        RAISE NOTICE '❌ FAIL: GUEST role not found';
        all_checks_passed := false;
    ELSE
        RAISE NOTICE '✅ PASS: GUEST role exists';
    END IF;

    -- Check 5: All tables exist
    IF NOT EXISTS(SELECT 1 FROM information_schema.tables WHERE table_name = 'permissions') THEN
        RAISE NOTICE '❌ FAIL: permissions table not found';
        all_checks_passed := false;
    ELSE
        RAISE NOTICE '✅ PASS: permissions table exists';
    END IF;

    IF NOT EXISTS(SELECT 1 FROM information_schema.tables WHERE table_name = 'roles') THEN
        RAISE NOTICE '❌ FAIL: roles table not found';
        all_checks_passed := false;
    ELSE
        RAISE NOTICE '✅ PASS: roles table exists';
    END IF;

    IF NOT EXISTS(SELECT 1 FROM information_schema.tables WHERE table_name = 'user_roles') THEN
        RAISE NOTICE '❌ FAIL: user_roles table not found';
        all_checks_passed := false;
    ELSE
        RAISE NOTICE '✅ PASS: user_roles table exists';
    END IF;

    IF NOT EXISTS(SELECT 1 FROM information_schema.tables WHERE table_name = 'role_permissions') THEN
        RAISE NOTICE '❌ FAIL: role_permissions table not found';
        all_checks_passed := false;
    ELSE
        RAISE NOTICE '✅ PASS: role_permissions table exists';
    END IF;

    -- Final summary
    RAISE NOTICE '';
    IF all_checks_passed THEN
        RAISE NOTICE '========================================';
        RAISE NOTICE '✅ ALL VALIDATION CHECKS PASSED!';
        RAISE NOTICE '========================================';
    ELSE
        RAISE NOTICE '========================================';
        RAISE NOTICE '❌ SOME VALIDATION CHECKS FAILED';
        RAISE NOTICE 'Please review the errors above';
        RAISE NOTICE '========================================';
    END IF;
END $$;

\echo ''

