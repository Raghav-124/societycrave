package com.oursociety.order;

import com.oursociety.listing.Listing; import com.oursociety.listing.ListingRepository;
import com.oursociety.membership.MembershipRepository; import com.oursociety.membership.SocietyMembership;
import com.oursociety.society.Society; import com.oursociety.user.User; import com.oursociety.user.UserRepository;
import org.springframework.http.HttpStatus; import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List; import java.util.UUID;

@Service
public class FoodOrderService {
 private final FoodOrderRepository orders; private final ListingRepository listings; private final MembershipRepository memberships; private final UserRepository users;
 public FoodOrderService(FoodOrderRepository orders,ListingRepository listings,MembershipRepository memberships,UserRepository users){this.orders=orders;this.listings=listings;this.memberships=memberships;this.users=users;}
 private User user(OidcUser principal){if(principal==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authentication required");return users.findByProviderAndProviderSubject("google",principal.getSubject()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Session user not found"));}
 private Society activeSociety(User user,UUID societyId){var active=memberships.findAllByUserId(user.getId()).stream().filter(m->m.getStatus()==SocietyMembership.Status.ACTIVE).toList();if(active.isEmpty())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Active society membership required");if(societyId!=null)return active.stream().filter(m->m.getSociety().getId().equals(societyId)).findFirst().map(SocietyMembership::getSociety).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,"Society membership is not active"));if(active.size()!=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"Explicit society selection is required");return active.get(0).getSociety();}
 @Transactional public FoodOrder place(OidcUser principal,UUID listingId,int quantity,UUID societyId){User user=user(principal);Society society=activeSociety(user,societyId);Listing listing=listings.findById(listingId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Listing not found"));if(listing.getStatus()!=Listing.Status.ACTIVE)throw new ResponseStatusException(HttpStatus.CONFLICT,"Listing is not active");if(!listing.getSociety().getId().equals(society.getId())||!listing.getChef().getSociety().getId().equals(society.getId()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Listing is outside your society");if(quantity<1)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Quantity must be positive");return orders.save(new FoodOrder(user,listing,quantity));}
 @Transactional(readOnly=true) public List<FoodOrder> mine(OidcUser principal){return orders.findAllByResident(user(principal));}
}
