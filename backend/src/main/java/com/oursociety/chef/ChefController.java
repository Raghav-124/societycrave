package com.oursociety.chef;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/chefs")
public class ChefController {
    private final ChefService chefs;
    public ChefController(ChefService chefs){this.chefs=chefs;}

    @PostMapping
    public ChefResponse onboard(@AuthenticationPrincipal OidcUser principal, @Valid @RequestBody OnboardRequest request) {
        Chef chef=chefs.onboard(principal, request.societyId(), request.displayName(), request.profilePhotoUrl(), request.bio(), request.cuisineSpecialties(), request.foodCategories(), request.pickupInstructions());
        return ChefResponse.from(chef);
    }

    @GetMapping("/me")
    public ChefResponse me(@AuthenticationPrincipal OidcUser principal) { return ChefResponse.from(chefs.forAuthenticatedUser(principal)); }

    public record OnboardRequest(UUID societyId,
        @NotBlank @Size(max=255) String displayName,
        @Size(max=2048) String profilePhotoUrl,
        @NotBlank @Size(max=2000) String bio,
        @NotBlank @Size(max=1000) String cuisineSpecialties,
        @NotBlank @Size(max=1000) String foodCategories,
        @NotBlank @Size(max=1000) String pickupInstructions) {}

    public record ChefResponse(UUID id, UUID societyId, String displayName, String profilePhotoUrl, String bio, String cuisineSpecialties, String foodCategories, String pickupInstructions) {
        static ChefResponse from(Chef chef){return new ChefResponse(chef.getId(),chef.getSociety().getId(),chef.getDisplayName(),chef.getProfilePhotoUrl(),chef.getBio(),chef.getCuisineSpecialties(),chef.getFoodCategories(),chef.getPickupInstructions());}
    }
}
