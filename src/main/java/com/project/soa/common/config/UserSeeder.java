package com.project.soa.common.config;

import com.project.soa.auth.user.User;
import com.project.soa.auth.user.UserRepository;
import com.project.soa.auth.user.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/*
 * Seeded accounts:
 *   admin@hotel.com   / Admin@123    — ADMIN
 *   manager@hotel.com / Manager@123  — MANAGER
 *   guest@hotel.com   / Guest@123    — GUEST
 *   john@hotel.com    / John@123     — GUEST
 */
@Configuration
public class UserSeeder {

    private static final Logger log = LoggerFactory.getLogger(UserSeeder.class);

    @Bean
    CommandLineRunner seedUsers(SeederHelper seederHelper) {
        return args -> {
            seederHelper.seed("Platform Admin",  "admin@hotel.com",   "Admin@123",   UserRole.ADMIN);
            seederHelper.seed("Hotel Manager",   "manager@hotel.com", "Manager@123", UserRole.MANAGER);
            seederHelper.seed("Test Guest",      "guest@hotel.com",   "Guest@123",   UserRole.GUEST);
            seederHelper.seed("John Doe",        "john@hotel.com",    "John@123",    UserRole.GUEST);
        };
    }

    @Component
    public static class SeederHelper {

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;

        public SeederHelper(UserRepository userRepository,
                            PasswordEncoder passwordEncoder) {
            this.userRepository = userRepository;
            this.passwordEncoder = passwordEncoder;
        }

        @Transactional
        public void seed(String name, String email, String password, UserRole role) {
            userRepository.findByEmail(email).ifPresentOrElse(
                    existing -> {
                        // User exists — just reset password, role and status (safe, no delete)
                        existing.setPasswordHash(passwordEncoder.encode(password));
                        existing.setRole(role);
                        existing.setStatus(User.UserStatus.ACTIVE);
                        userRepository.save(existing);
                        log.info("Reset seeded account: {} ({})", email, role);
                    },
                    () -> {
                        // User does not exist — create fresh
                        User user = new User();
                        user.setName(name);
                        user.setEmail(email);
                        user.setPasswordHash(passwordEncoder.encode(password));
                        user.setRole(role);
                        user.setStatus(User.UserStatus.ACTIVE);
                        userRepository.save(user);
                        log.info("Created seeded account: {} ({})", email, role);
                    }
            );
        }
    }
}