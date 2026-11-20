package com.aegiscare.service;

import com.aegiscare.entity.UserAccount;
import com.aegiscare.repository.UserAccountRepository;
import com.aegiscare.security.JwtTokenService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserAccountRepository accounts;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokens;

    public AuthService(UserAccountRepository accounts, PasswordEncoder passwordEncoder, JwtTokenService tokens) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
    }

    @Transactional(readOnly = true)
    public LoginResult login(String email, String password) {
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new BadCredentialsException("Email or password is incorrect");
        }
        UserAccount account = accounts.findByEmailIgnoreCase(email.trim())
                .filter(UserAccount::isEnabled)
                .filter(candidate -> passwordEncoder.matches(password, candidate.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Email or password is incorrect"));
        JwtTokenService.IssuedToken issued = tokens.issue(account);
        return new LoginResult(issued.accessToken(), issued.expiresIn(), UserView.from(account));
    }

    @Transactional(readOnly = true)
    public UserView currentUser(String userId) {
        return accounts.findById(userId).filter(UserAccount::isEnabled)
                .map(UserView::from)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED, "Account is disabled or no longer exists"));
    }

    public record LoginResult(String accessToken, long expiresIn, UserView user) { }
    public record UserView(String id, String email, String fullName, String role, String hospitalId) {
        public static UserView from(UserAccount account) {
            return new UserView(account.getId(), account.getEmail(), account.getFullName(),
                    account.getRole().name(), account.getHospitalId());
        }
    }
}
