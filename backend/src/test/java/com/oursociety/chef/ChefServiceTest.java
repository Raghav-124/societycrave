package com.oursociety.chef;

import com.oursociety.membership.MembershipRepository;
import com.oursociety.user.User;
import com.oursociety.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChefServiceTest {
 @Mock ChefRepository chefs;
 @Mock MembershipRepository memberships;
 @Mock UserRepository users;
 @Mock OidcUser principal;

 @Test void rejectsOnboardingWithoutActiveMembership(){
  var service=new ChefService(chefs,memberships,users);
  var user=new User("google","subject","a@example.com","A");
  when(principal.getSubject()).thenReturn("subject");
  when(users.findByProviderAndProviderSubject("google","subject")).thenReturn(Optional.of(user));
  when(memberships.findAllByUserId(user.getId())).thenReturn(List.of());
  assertThatThrownBy(() -> service.onboard(principal,null,"Chef",null,"Bio","Indian","Thali","Pickup"))
   .isInstanceOf(ResponseStatusException.class).hasMessageContaining("Active society membership required");
 }
}
