-- Fix admin user password hash
-- This script updates the admin user's password hash to a valid BCrypt hash for "admin"
-- Run this script on the existing database to fix the password issue

-- Update admin user with correct BCrypt hash for password "admin"
-- New hash: $2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b3.
UPDATE users
SET hashed_password = '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b3.'
WHERE username = 'admin';

-- Verify the update
SELECT username, email,
       LEFT(hashed_password, 20) as hash_prefix,
       LENGTH(hashed_password) as hash_length,
       enabled
FROM users
WHERE username = 'admin';
