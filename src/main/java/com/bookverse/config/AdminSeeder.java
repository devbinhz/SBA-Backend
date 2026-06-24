package com.bookverse.config;

import com.bookverse.entity.User;
import com.bookverse.enums.UserRole;
import com.bookverse.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Component
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BookverseAdminProperties adminProperties;

    public AdminSeeder(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       BookverseAdminProperties adminProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminProperties = adminProperties;
    }

    @Override
    @Transactional
    public void run(String... args) {
        String normalizedEmail = normalizeEmail(adminProperties.email());
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            log.info("Admin seed skipped because account already exists: {}", normalizedEmail);
            return;
        }

        User admin = new User();
        admin.setEmail(normalizedEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminProperties.password()));
        admin.setFullName(adminProperties.fullName().trim());
        admin.setRole(UserRole.ADMIN);
        admin.setEnabled(true);
        admin.setEmailVerified(true);
        admin.setEmailVerifiedAt(Instant.now());
        userRepository.save(admin);
        log.info("Admin account seeded: {}", normalizedEmail);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}

