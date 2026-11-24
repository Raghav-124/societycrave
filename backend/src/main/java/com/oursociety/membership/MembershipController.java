package com.oursociety.membership;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequestMapping("/api/memberships")
public class MembershipController {
    private final MembershipService membershipService;
    public MembershipController(MembershipService membershipService){this.membershipService=membershipService;}
    @PostMapping MembershipResponse join(@AuthenticationPrincipal OidcUser principal,@Valid @RequestBody JoinRequest request) {
        var membership = membershipService.join(principal, request.societyId(), request.unitNumber());
        return new MembershipResponse(membership.getId(), membership.getStatus().name(), membership.getSociety().getId(), membership.getUnitNumber());
    }
    record JoinRequest(@NotNull UUID societyId,
                       @NotBlank @jakarta.validation.constraints.Size(max = 32)
                       @jakarta.validation.constraints.Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9 /-]*$")
                       String unitNumber,
                       @AssertTrue boolean selfDeclared) {}
    record MembershipResponse(UUID id, String status, UUID societyId, String unitNumber) {}
    @GetMapping("/me") java.util.List<MembershipResponse> me(@AuthenticationPrincipal OidcUser principal){
        return membershipService.membershipsFor(principal).stream()
            .map(m -> new MembershipResponse(m.getId(), m.getStatus().name(), m.getSociety().getId(), m.getUnitNumber()))
            .toList();
    }
}
