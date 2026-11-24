package com.oursociety.membership;

import com.oursociety.society.SocietyRepository;
import com.oursociety.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {
    @Mock MembershipRepository memberships;
    @Mock SocietyRepository societies;
    @Mock UserRepository users;
    @Mock OidcUser principal;

    @Test
    void rejectsMembershipWithoutAnAuthenticatedSession() {
        var service = new MembershipService(memberships, societies, users);

        assertThatThrownBy(() -> service.join(null, UUID.randomUUID(), "A-101"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Authentication required");
    }
}
