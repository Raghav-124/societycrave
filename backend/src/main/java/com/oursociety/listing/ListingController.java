package com.oursociety.listing;

import jakarta.validation.Valid; import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import java.util.List; import java.util.UUID;

@RestController @RequestMapping("/api/listings")
public class ListingController {
 private final ListingService listings;
 public ListingController(ListingService listings){this.listings=listings;}
 @PostMapping public ListingResponse create(@AuthenticationPrincipal OidcUser principal,@Valid @RequestBody CreateRequest request){return ListingResponse.from(listings.create(principal,request.title(),request.description(),request.pricePaise(),request.pickupInstructions()));}
 @GetMapping("/discover") public List<ListingResponse> discover(@AuthenticationPrincipal OidcUser principal,@RequestParam(required=false) UUID societyId){return listings.discover(principal,societyId).stream().map(ListingResponse::from).toList();}
 @GetMapping("/mine") public List<ListingResponse> mine(@AuthenticationPrincipal OidcUser principal){return listings.mine(principal).stream().map(ListingResponse::from).toList();}
 public record CreateRequest(@NotBlank @Size(max=255) String title,@NotBlank @Size(max=2000) String description,@Positive int pricePaise,@Size(max=1000) String pickupInstructions){}
 public record ListingResponse(UUID id,UUID chefId,UUID societyId,String title,String description,int pricePaise,String pickupInstructions,String status){static ListingResponse from(Listing l){return new ListingResponse(l.getId(),l.getChef().getId(),l.getSociety().getId(),l.getTitle(),l.getDescription(),l.getPricePaise(),l.getPickupInstructions(),l.getStatus().name());}}
}
