package com.oursociety.auth;

import com.oursociety.user.User;
import com.oursociety.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserRepository users;
    @Mock OidcUser principal;

    @Test
    void createsGoogleUserFromOidcSubjectWhenFirstSeen() {
        when(principal.getSubject()).thenReturn("google-sub");
        when(principal.getEmail()).thenReturn("person@example.com");
        when(principal.getFullName()).thenReturn("Person");
        when(users.findByProviderAndProviderSubject("google", "google-sub")).thenReturn(Optional.empty());
        when(users.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = new AuthService(users).findOrCreateGoogleUser(principal);

        assertThat(user.getEmail()).isEqualTo("person@example.com");
        verify(users).saveAndFlush(any(User.class));
    }

    @Test
    void reusesExistingUserByProviderSubjectWithoutEmailLinking() {
        User existing = new User("google", "google-sub", "old@example.com", "Person");
        when(principal.getSubject()).thenReturn("google-sub");
        when(users.findByProviderAndProviderSubject("google", "google-sub")).thenReturn(Optional.of(existing));

        assertThat(new AuthService(users).findOrCreateGoogleUser(principal)).isSameAs(existing);
        verify(users, never()).saveAndFlush(any());
    }
}
