-- MyQuiz Default Admin User Initialization
-- This script creates a default admin user for initial login
-- Username: admin
-- Password: admin
-- Bcrypt hash: $2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG13AriDDmKvkPPrF6

-- Insert default admin user (only if not exists)
INSERT INTO users (username, email, hashed_password)
SELECT 'admin', 'admin@myquiz.local', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG13AriDDmKvkPPrF6'
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE username = 'admin'
);

