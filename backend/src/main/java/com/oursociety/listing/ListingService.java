package com.oursociety.listing;

import com.oursociety.chef.Chef; import com.oursociety.chef.ChefRepository;
import com.oursociety.membership.MembershipRepository; import com.oursociety.membership.SocietyMembership;
import com.oursociety.society.Society; import com.oursociety.user.User; import com.oursociety.user.UserRepository;
import org.springframework.http.HttpStatus; import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;

@Service
public class ListingService {
 private final ListingRepository listings; private final ChefRepository chefs; private final MembershipRepository memberships; private final UserRepository users;
 public ListingService(ListingRepository listings,ChefRepository chefs,MembershipRepository memberships,UserRepository users){this.listings=listings;this.chefs=chefs;this.memberships=memberships;this.users=users;}
 private User user(OidcUser principal){if(principal==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authentication required");return users.findByProviderAndProviderSubject("google",principal.getSubject()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Session user not found"));}
 private Society activeSociety(User user, UUID societyId){var active=memberships.findAllByUserId(user.getId()).stream().filter(m->m.getStatus()==SocietyMembership.Status.ACTIVE).toList();if(active.isEmpty())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Active society membership required");if(societyId!=null){return active.stream().filter(m->m.getSociety().getId().equals(societyId)).findFirst().map(SocietyMembership::getSociety).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,"Society membership is not active"));}if(active.size()!=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"Explicit society selection is required");return active.get(0).getSociety();}
 @Transactional public Listing create(OidcUser principal,String title,String description,int pricePaise,String pickupInstructions){User user=user(principal);Chef chef=chefs.findByUser(user).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Chef profile not found"));Society society=activeSociety(user,chef.getSociety().getId());if(!chef.getSociety().getId().equals(society.getId()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Chef does not belong to an active society membership");return listings.save(new Listing(chef,title.trim(),description.trim(),pricePaise,pickupInstructions));}
 @Transactional(readOnly=true) public List<Listing> discover(OidcUser principal,UUID societyId){User user=user(principal);Society society=activeSociety(user,societyId);return listings.findAllBySocietyAndStatus(society,Listing.Status.ACTIVE);}
 @Transactional(readOnly=true) public List<Listing> mine(OidcUser principal){Chef chef=chefs.findByUser(user(principal)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Chef profile not found"));return listings.findAllByChef(chef);}
}
