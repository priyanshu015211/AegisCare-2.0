package com.aegiscare.config;

import com.aegiscare.entity.UserAccount;
import com.aegiscare.repository.UserAccountRepository;
import com.aegiscare.security.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "aegiscare.security.bootstrap-admin.enabled", havingValue = "true")
public class BootstrapAdminInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminInitializer.class);
    private final UserAccountRepository accounts;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String fullName;

    public BootstrapAdminInitializer(UserAccountRepository accounts, PasswordEncoder passwordEncoder,
                                     @Value("${aegiscare.security.bootstrap-admin.email:}") String email,
                                     @Value("${aegiscare.security.bootstrap-admin.password:}") String password,
                                     @Value("${aegiscare.security.bootstrap-admin.full-name:AegisCare Administrator}") String fullName) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email == null || email.isBlank() || password == null || password.length() < 12
                || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("Bootstrap admin is enabled but its email/password is invalid. Set AEGISCARE_BOOTSTRAP_ADMIN_EMAIL and a 12-72 character AEGISCARE_BOOTSTRAP_ADMIN_PASSWORD.");
        }
        if (accounts.existsByEmailIgnoreCase(email.trim())) {
            log.info("Bootstrap administrator already exists; no credentials were changed.");
            return;
        }
        UserAccount account = new UserAccount();
        account.setEmail(email.trim().toLowerCase());
        account.setFullName(fullName == null || fullName.isBlank() ? "AegisCare Administrator" : fullName.trim());
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setRole(UserRole.ADMIN);
        account.setEnabled(true);
        accounts.save(account);
        log.warn("Bootstrap administrator account created for {}. Change its password after the first login.", account.getEmail());
    }
}
