package com.unitbv.myquiz.iam.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Fallback initializer for admin user and role assignment.
 *
 * Primary clean-install seeding is done in data/init-database.sql.
 * This runner repairs environments where SQL seeding was skipped
 * (for example, existing databases or partial restores).
 */
@Component
public class AdminUserInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserInitializer.class);
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_ROLE = "ADMINISTRATOR";
    private static final String ADMIN_DEFAULT_PASSWORD = "admin";
    private static final String LEGACY_INCORRECT_ADMIN_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMye0qCKG.J5S5fJMzr6xF.L5CzOgPk3oa2";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private CacheManager cacheManager;

    /**
     * Returns true only if the hash value looks like a BCrypt hash
     * ($2a$, $2b$ or $2y$ prefix).  Never throws.
     */
    private boolean isBCryptHash(String hash) {
        if (hash == null || hash.length() < 7) {
            return false;
        }
        return hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$");
    }

    /**
     * Returns true only if the hash is a valid BCrypt hash AND it matches
     * the given raw password.  Never throws – any exception is treated as
     * a non-match so the caller will regenerate the hash.
     */
    private boolean hashMatchesPassword(BCryptPasswordEncoder encoder, String rawPassword, String hash) {
        if (!isBCryptHash(hash)) {
            return false;
        }
        try {
            return encoder.matches(rawPassword, hash);
        } catch (Exception ex) {
            log.warn("Exception while validating BCrypt hash, treating as mismatch: {}", ex.getMessage());
            return false;
        }
    }

    /** Evict all admin-related entries from the Caffeine 'users' cache. */
    private void evictAdminFromCache() {
        if (cacheManager == null) {
            return;
        }
        try {
            Cache usersCache = cacheManager.getCache("users");
            if (usersCache != null) {
                usersCache.evict(ADMIN_USERNAME);
                usersCache.evict("all");   // evict the getAllUsers() entry too
                log.debug("Evicted admin from 'users' cache after hash repair");
            }
        } catch (Exception ex) {
            log.warn("Could not evict admin from cache: {}", ex.getMessage());
        }
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            // Check if admin user already exists
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ?",
                Integer.class,
                ADMIN_USERNAME
            );

            if (count != null && count == 0) {
                // Create BCrypt hash for password "admin"
                BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
                String hashedPassword = encoder.encode(ADMIN_DEFAULT_PASSWORD);

                // Insert default admin user
                jdbcTemplate.update(
                    "INSERT INTO users (username, email, hashed_password) VALUES (?, ?, ?)",
                    ADMIN_USERNAME,
                    "admin@myquiz.local",
                    hashedPassword
                );

                // Assign ADMINISTRATOR role to admin user
                int rolesAssigned = jdbcTemplate.update(
                    "INSERT INTO user_roles (user_id, role_id) " +
                    "SELECT u.user_id, r.role_id FROM users u " +
                    "CROSS JOIN roles r " +
                    "WHERE u.username = ? AND r.name = ? " +
                    "ON CONFLICT DO NOTHING",
                    ADMIN_USERNAME,
                    ADMIN_ROLE
                );

                log.info("✅ Default admin user created successfully");
                log.info("   Username: {}", ADMIN_USERNAME);
                log.info("   Password: {}", ADMIN_DEFAULT_PASSWORD);
                if (rolesAssigned > 0) {
                    log.info("   ✅ ADMINISTRATOR role assigned");
                } else {
                    log.warn("   ⚠️  Failed to assign ADMINISTRATOR role - role may not exist yet");
                }
                log.info("   ⚠️  IMPORTANT: Change the default password after first login!");
                evictAdminFromCache();
            } else {
                log.info("ℹ️  Admin user already exists, skipping initialization");
                
                // Ensure admin user has ADMINISTRATOR role (in case it was missed)
                Integer roleCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM user_roles ur " +
                    "JOIN users u ON ur.user_id = u.user_id " +
                    "JOIN roles r ON ur.role_id = r.role_id " +
                    "WHERE u.username = ? AND r.name = ?",
                    Integer.class,
                    ADMIN_USERNAME,
                    ADMIN_ROLE
                );
                
                if (roleCount != null && roleCount == 0) {
                    int rolesAssigned = jdbcTemplate.update(
                        "INSERT INTO user_roles (user_id, role_id) " +
                        "SELECT u.user_id, r.role_id FROM users u " +
                        "CROSS JOIN roles r " +
                        "WHERE u.username = ? AND r.name = ? " +
                        "ON CONFLICT DO NOTHING",
                        ADMIN_USERNAME,
                        ADMIN_ROLE
                    );
                    if (rolesAssigned > 0) {
                        log.info("   ✅ ADMINISTRATOR role assigned to existing admin user");
                    }
                }

                BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

                // Repair legacy incorrect admin hash from older init scripts.
                int repaired = jdbcTemplate.update(
                    "UPDATE users SET hashed_password = ? WHERE username = ? AND hashed_password = ?",
                    encoder.encode(ADMIN_DEFAULT_PASSWORD),
                    ADMIN_USERNAME,
                    LEGACY_INCORRECT_ADMIN_HASH
                );
                if (repaired > 0) {
                    log.info("✅ Repaired legacy admin password hash for user '{}'", ADMIN_USERNAME);
                    evictAdminFromCache();
                }

                // Defensive repair: fetch the current hash and validate it thoroughly.
                // Uses isBCryptHash() + hashMatchesPassword() so we never rely on
                // BCryptPasswordEncoder.matches() silently swallowing or throwing
                // exceptions for non-BCrypt strings.
                String currentHash = jdbcTemplate.queryForObject(
                    "SELECT hashed_password FROM users WHERE username = ?",
                    String.class,
                    ADMIN_USERNAME
                );

                if (!hashMatchesPassword(encoder, ADMIN_DEFAULT_PASSWORD, currentHash)) {
                    log.warn("⚠️  Admin password hash is invalid or does not match the default password. " +
                             "Hash prefix: '{}'. Regenerating...",
                             currentHash != null ? currentHash.substring(0, Math.min(7, currentHash.length())) : "null");

                    int repairedAny = jdbcTemplate.update(
                        "UPDATE users SET hashed_password = ? WHERE username = ?",
                        encoder.encode(ADMIN_DEFAULT_PASSWORD),
                        ADMIN_USERNAME
                    );
                    if (repairedAny > 0) {
                        log.info("✅ Repaired admin password hash mismatch for user '{}'", ADMIN_USERNAME);
                        evictAdminFromCache();
                    } else {
                        log.error("❌ Failed to update admin password hash – UPDATE affected 0 rows");
                    }
                }
            }
        } catch (Exception e) {
            log.error("❌ Failed to initialize admin user: {}", e.getMessage(), e);
            // Don't throw exception - allow app to continue even if this fails
        }
    }
}
