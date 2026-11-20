package com.aegiscare.repository;

import com.aegiscare.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, String> {
    Optional<UserAccount> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<UserAccount> findAllByOrderByCreatedAtDesc();
}
