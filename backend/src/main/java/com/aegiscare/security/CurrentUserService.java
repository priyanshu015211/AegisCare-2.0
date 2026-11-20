package com.aegiscare.security;

import com.aegiscare.entity.UserAccount;
import com.aegiscare.repository.UserAccountRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CurrentUserService {
    private final UserAccountRepository accounts;

    public CurrentUserService(UserAccountRepository accounts) { this.accounts = accounts; }

    private Jwt jwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("Authenticated user context is unavailable");
        }
        return jwt;
    }

    public UserAccount account() {
        String id = jwt().getClaimAsString("userId");
        return accounts.findById(id == null ? "" : id).filter(UserAccount::isEnabled)
                .orElseThrow(() -> new AccessDeniedException("Account is inactive or unavailable"));
    }

    public String userId() { return account().getId(); }
    public String email() { return account().getEmail(); }
    public String fullName() { return account().getFullName(); }
    public String hospitalId() { return account().getHospitalId(); }
    public List<String> roles() { return List.of(account().getRole().name()); }
    public boolean isAdmin() { return account().getRole() == UserRole.ADMIN; }

    public String requireHospitalId() {
        String hospitalId = hospitalId();
        if (hospitalId == null || hospitalId.isBlank()) {
            throw new AccessDeniedException("Your account is not assigned to a hospital");
        }
        return hospitalId;
    }
}
