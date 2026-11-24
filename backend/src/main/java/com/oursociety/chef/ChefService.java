package com.oursociety.chef;

import com.oursociety.membership.MembershipRepository;
import com.oursociety.membership.SocietyMembership;
import com.oursociety.society.Society;
import com.oursociety.user.User;
import com.oursociety.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class ChefService {
    private final ChefRepository chefs;
    private final MembershipRepository memberships;
    private final UserRepository users;

    public ChefService(ChefRepository chefs, MembershipRepository memberships, UserRepository users) {
        this.chefs=chefs; this.memberships=memberships; this.users=users;
    }

    private User authenticatedUser(OidcUser principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        return users.findByProviderAndProviderSubject("google", principal.getSubject())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session user not found"));
    }

    @Transactional(readOnly=true)
    public List<SocietyMembership> activeMemberships(OidcUser principal) {
        User user=authenticatedUser(principal);
        return memberships.findAllByUserId(user.getId()).stream()
            .filter(m -> m.getStatus()==SocietyMembership.Status.ACTIVE).toList();
    }

    public Society resolveActiveSociety(User user, UUID societyId) {
        List<SocietyMembership> active=memberships.findAllByUserId(user.getId()).stream()
            .filter(m -> m.getStatus()==SocietyMembership.Status.ACTIVE).toList();
        if (active.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Active society membership required");
        if (societyId != null) {
            return active.stream().filter(m -> m.getSociety().getId().equals(societyId)).findFirst()
                .map(SocietyMembership::getSociety)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Society membership is not active"));
        }
        if (active.size()==1) return active.get(0).getSociety();
        throw new ResponseStatusException(HttpStatus.CONFLICT, "societyId is required when multiple active memberships exist");
    }

    @Transactional
    public Chef onboard(OidcUser principal, UUID societyId, String displayName, String profilePhotoUrl, String bio, String cuisineSpecialties, String foodCategories, String pickupInstructions) {
        User user=authenticatedUser(principal);
        Society society=resolveActiveSociety(user, societyId);
        Chef existing=chefs.findByUser(user).orElse(null);
        if (existing != null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Chef profile already exists");
        return chefs.save(new Chef(user,society,displayName.trim(),profilePhotoUrl,bio.trim(),cuisineSpecialties.trim(),foodCategories.trim(),pickupInstructions.trim()));
    }

    @Transactional(readOnly=true)
    public Chef forAuthenticatedUser(OidcUser principal) {
        Chef chef=chefs.findByUser(authenticatedUser(principal)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Chef profile not found"));
        return chef;
    }
}
