package com.oursociety.auth;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import com.oursociety.membership.MembershipService;

class AuthControllerTest {
    @Test
    void returnsUnauthorizedWhenThereIsNoOidcSession() {
        var response = new AuthController(mock(AuthService.class), mock(MembershipService.class)).me(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
