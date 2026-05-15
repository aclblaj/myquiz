import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerateHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        String password = "admin";
        System.out.println("Generating new BCrypt hash for password: " + password);
        System.out.println();
        
        // Generate multiple hashes to show they're different each time
        for (int i = 0; i < 3; i++) {
            String hash = encoder.encode(password);
            System.out.println("Hash " + (i+1) + ": " + hash);
            System.out.println("  Length: " + hash.length());
            System.out.println("  Matches: " + encoder.matches(password, hash));
            System.out.println();
        }
        
        // Test existing hashes from database
        System.out.println("\n=== Testing existing database hash ===");
        String dbHash = "$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b3.";
        System.out.println("Database hash: " + dbHash);
        System.out.println("Length: " + dbHash.length());
        System.out.println("Matches 'admin': " + encoder.matches("admin", dbHash));
    }
}
