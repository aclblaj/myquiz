package com.unitbv.myquiz.iam.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Initializes default admin user on application startup if not exists.
 * Username: admin
 * Password: admin
 */
@Component
public class AdminUserInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserInitializer.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        try {
            // Check if admin user already exists
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ?",
                Integer.class,
                "admin"
            );

            if (count != null && count == 0) {
                // Create BCrypt hash for password "admin"
                BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
                String hashedPassword = encoder.encode("admin");

                // Insert default admin user
                jdbcTemplate.update(
                    "INSERT INTO users (username, email, hashed_password) VALUES (?, ?, ?)",
                    "admin",
                    "admin@myquiz.local",
                    hashedPassword
                );

                log.info("✅ Default admin user created successfully");
                log.info("   Username: admin");
                log.info("   Password: admin");
                log.info("   ⚠️  IMPORTANT: Change the default password after first login!");
            } else {
                log.info("ℹ️  Admin user already exists, skipping initialization");
            }
        } catch (Exception e) {
            log.error("❌ Failed to initialize admin user: {}", e.getMessage(), e);
            // Don't throw exception - allow app to continue even if this fails
        }
    }
}

