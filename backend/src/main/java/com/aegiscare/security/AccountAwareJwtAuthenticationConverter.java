package com.aegiscare.security;

import com.aegiscare.entity.UserAccount;
import com.aegiscare.repository.UserAccountRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import java.util.List;

/** Resolves current account status and role on every authenticated request. */
@Component
public class AccountAwareJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final UserAccountRepository accounts;

    public AccountAwareJwtAuthenticationConverter(UserAccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String userId = jwt.getClaimAsString("userId");
        UserAccount account = accounts.findById(userId == null ? "" : userId)
                .filter(UserAccount::isEnabled)
                .orElseThrow(() -> new InvalidBearerTokenException("Account is inactive or no longer exists"));
        return new JwtAuthenticationToken(jwt,
                List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole().name())),
                account.getEmail());
    }
}
