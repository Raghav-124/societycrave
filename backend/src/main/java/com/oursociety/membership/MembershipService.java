package com.oursociety.membership;

import com.oursociety.society.SocietyRepository;
import com.oursociety.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class MembershipService {
    private final MembershipRepository memberships;
    private final SocietyRepository societies;
    private final UserRepository users;

    public MembershipService(MembershipRepository memberships, SocietyRepository societies, UserRepository users) {
        this.memberships = memberships;
        this.societies = societies;
        this.users = users;
    }

    @Transactional
    public SocietyMembership join(OidcUser principal, UUID societyId, String unitNumber) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        var user = users.findByProviderAndProviderSubject("google", principal.getSubject())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session user not found"));
        var society = societies.findById(societyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Society not found"));
        if (memberships.existsByUserIdAndSocietyId(user.getId(), society.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Membership already exists");
        }
        try {
            return memberships.saveAndFlush(new SocietyMembership(user, society, unitNumber.trim()));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Membership already exists", exception);
        }
    }

    @Transactional(readOnly = true)
    public java.util.List<SocietyMembership> membershipsFor(OidcUser principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        var user = users.findByProviderAndProviderSubject("google", principal.getSubject())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session user not found"));
        return memberships.findAllByUserId(user.getId());
    }
}
