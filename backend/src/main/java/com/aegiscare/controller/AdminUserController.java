package com.aegiscare.controller;

import com.aegiscare.entity.UserAccount;
import com.aegiscare.repository.HospitalRepository;
import com.aegiscare.repository.UserAccountRepository;
import com.aegiscare.security.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {
    private final UserAccountRepository accounts;
    private final HospitalRepository hospitals;
    private final PasswordEncoder passwordEncoder;

    public AdminUserController(UserAccountRepository accounts, HospitalRepository hospitals,
                               PasswordEncoder passwordEncoder) {
        this.accounts = accounts;
        this.hospitals = hospitals;
        this.passwordEncoder = passwordEncoder;
    }

    public record CreateUserRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(min = 2, max = 120) String fullName,
            @NotBlank @Size(min = 12, max = 72) String password,
            @NotNull UserRole role,
            String hospitalId
    ) { }

    public record UserSummary(String id, String email, String fullName, String role,
                              String hospitalId, boolean enabled, String createdAt) { }

    @GetMapping
    public List<UserSummary> list() {
        return accounts.findAllByOrderByCreatedAtDesc().stream().map(AdminUserController::toSummary).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserSummary create(@Valid @RequestBody CreateUserRequest request) {
        String email = request.email().trim().toLowerCase();
        if (request.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must not exceed 72 UTF-8 bytes");
        }
        if (accounts.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        if (request.role() != UserRole.ADMIN && (request.hospitalId() == null || request.hospitalId().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A hospital assignment is required for clinical and coordination accounts");
        }
        if (request.hospitalId() != null && !request.hospitalId().isBlank()
                && !hospitals.existsById(request.hospitalId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The selected hospital does not exist");
        }
        UserAccount account = new UserAccount();
        account.setEmail(email);
        account.setFullName(request.fullName().trim());
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        account.setRole(request.role());
        account.setHospitalId(blankToNull(request.hospitalId()));
        account.setEnabled(true);
        return toSummary(accounts.save(account));
    }

    @PatchMapping("/{id}/disable")
    public UserSummary disable(@PathVariable String id) {
        UserAccount account = accounts.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        if (account.getRole() == UserRole.ADMIN && accounts.findAll().stream()
                .filter(UserAccount::isEnabled).filter(a -> a.getRole() == UserRole.ADMIN).count() <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The last active administrator cannot be disabled");
        }
        account.setEnabled(false);
        return toSummary(accounts.save(account));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static UserSummary toSummary(UserAccount account) {
        return new UserSummary(account.getId(), account.getEmail(), account.getFullName(),
                account.getRole().name(), account.getHospitalId(), account.isEnabled(), account.getCreatedAt().toString());
    }
}
