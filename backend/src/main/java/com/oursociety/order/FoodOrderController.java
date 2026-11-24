package com.oursociety.order;

import jakarta.validation.Valid; import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import java.util.List; import java.util.UUID;

@RestController @RequestMapping("/api/orders")
public class FoodOrderController {
 private final FoodOrderService orders;
 public FoodOrderController(FoodOrderService orders){this.orders=orders;}
 @PostMapping public OrderResponse place(@AuthenticationPrincipal OidcUser principal,@Valid @RequestBody PlaceRequest request){return OrderResponse.from(orders.place(principal,request.listingId(),request.quantity(),request.societyId()));}
 @GetMapping("/mine") public List<OrderResponse> mine(@AuthenticationPrincipal OidcUser principal){return orders.mine(principal).stream().map(OrderResponse::from).toList();}
 public record PlaceRequest(@NotNull UUID listingId,@Positive int quantity,UUID societyId){}
 public record OrderResponse(UUID id,UUID societyId,UUID chefId,UUID listingId,int quantity,String status){static OrderResponse from(FoodOrder o){return new OrderResponse(o.getId(),o.getSociety().getId(),o.getChef().getId(),o.getListing().getId(),o.getQuantity(),o.getStatus().name());}}
}
