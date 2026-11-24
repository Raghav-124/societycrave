package com.oursociety.auth;

import com.oursociety.user.User;
import com.oursociety.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository users;

    public AuthService(UserRepository users) {
        this.users = users;
    }

    public User findOrCreateGoogleUser(OidcUser principal) {
        return users.findByProviderAndProviderSubject("google", principal.getSubject())
            .orElseGet(() -> createGoogleUserSafely(principal));
    }

    private User createGoogleUserSafely(OidcUser principal) {
        try {
            return users.saveAndFlush(new User(
                "google",
                principal.getSubject(),
                principal.getEmail(),
                principal.getFullName()
            ));
        } catch (DataIntegrityViolationException exception) {
            return users.findByProviderAndProviderSubject("google", principal.getSubject())
                .orElseThrow(() -> exception);
        }
    }
}
