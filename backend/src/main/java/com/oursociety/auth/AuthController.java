package com.oursociety.auth;

import com.oursociety.membership.MembershipService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final MembershipService membershipService;
    public AuthController(AuthService authService, MembershipService membershipService){this.authService=authService;this.membershipService=membershipService;}
    @GetMapping("/me") ResponseEntity<?> me(@AuthenticationPrincipal OidcUser principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        var user = authService.findOrCreateGoogleUser(principal);
        boolean activeMembership = membershipService.membershipsFor(principal).stream()
            .anyMatch(membership -> membership.getStatus() == com.oursociety.membership.SocietyMembership.Status.ACTIVE);
        return ResponseEntity.ok(new MeResponse(user.getId(), user.getEmail(), user.getDisplayName(), activeMembership ? "ACTIVE_MEMBERSHIP" : "ONBOARDING_REQUIRED"));
    }
    /*
     * Server sessions do not expose refresh tokens. Session continuity is
     * managed by the HttpOnly session cookie, so no refresh endpoint is needed.
     */
    record MeResponse(java.util.UUID id, String email, String displayName, String onboardingStatus) {}
}
