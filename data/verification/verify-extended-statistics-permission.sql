-- =====================================================
-- Verification Script for IAM Statistics Permission Issue
-- =====================================================

\echo '===== STEP 1: Check if VIEW_EXTENDED_STATISTICS permission exists ====='
SELECT permission_id, name, description, resource, action
FROM permissions
WHERE name = 'VIEW_EXTENDED_STATISTICS';

\echo ''
\echo '===== STEP 2: Check if admin user exists ====='
SELECT user_id, username, email, enabled
FROM users
WHERE username = 'admin';

\echo ''
\echo '===== STEP 3: Check if ADMINISTRATOR role exists ====='
SELECT role_id, name, description
FROM roles
WHERE name = 'ADMINISTRATOR';

\echo ''
\echo '===== STEP 4: Check if admin user has ADMINISTRATOR role ====='
SELECT u.user_id, u.username, r.role_id, r.name as role_name, ur.assigned_at
FROM users u
JOIN user_roles ur ON u.user_id = ur.user_id
JOIN roles r ON ur.role_id = r.role_id
WHERE u.username = 'admin';

\echo ''
\echo '===== STEP 5: Check if ADMINISTRATOR role has VIEW_EXTENDED_STATISTICS permission ====='
SELECT r.role_id, r.name as role_name, p.permission_id, p.name as permission_name
FROM roles r
JOIN role_permissions rp ON r.role_id = rp.role_id
JOIN permissions p ON rp.permission_id = p.permission_id
WHERE r.name = 'ADMINISTRATOR' AND p.name = 'VIEW_EXTENDED_STATISTICS';

\echo ''
\echo '===== STEP 6: List ALL permissions for ADMINISTRATOR role ====='
SELECT p.name as permission_name, p.resource, p.action
FROM roles r
JOIN role_permissions rp ON r.role_id = rp.role_id
JOIN permissions p ON rp.permission_id = p.permission_id
WHERE r.name = 'ADMINISTRATOR'
ORDER BY p.resource, p.action;

\echo ''
\echo '===== STEP 7: Count permissions for admin user (should see all ADMINISTRATOR permissions) ====='
SELECT COUNT(DISTINCT p.permission_id) as total_permissions
FROM users u
JOIN user_roles ur ON u.user_id = ur.user_id
JOIN roles r ON ur.role_id = r.role_id
JOIN role_permissions rp ON r.role_id = rp.role_id
JOIN permissions p ON rp.permission_id = p.permission_id
WHERE u.username = 'admin';

\echo ''
\echo '===== STEP 8: List all permissions for admin user ====='
SELECT DISTINCT p.name as permission_name
FROM users u
JOIN user_roles ur ON u.user_id = ur.user_id
JOIN roles r ON ur.role_id = r.role_id
JOIN role_permissions rp ON r.role_id = rp.role_id
JOIN permissions p ON rp.permission_id = p.permission_id
WHERE u.username = 'admin'
ORDER BY p.name;

\echo ''
\echo '===== VERIFICATION COMPLETE ====='
\echo 'Expected results:'
\echo '- Step 1: Should return 1 row with VIEW_EXTENDED_STATISTICS permission'
\echo '- Step 2: Should return 1 row with admin user'
\echo '- Step 3: Should return 1 row with ADMINISTRATOR role'
\echo '- Step 4: Should return 1 row showing admin has ADMINISTRATOR role'
\echo '- Step 5: Should return 1 row showing ADMINISTRATOR has VIEW_EXTENDED_STATISTICS'
\echo '- Step 6: Should return all permissions in the system (dynamic total)'
\echo '- Step 7: Should return count equal to total permissions from table permissions'
\echo '- Step 8: Should list all 16 permissions including VIEW_EXTENDED_STATISTICS'
