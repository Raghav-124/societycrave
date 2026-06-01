package com.raghav.societycrave.controller;

import com.raghav.societycrave.dto.auth.AuthResponse;
import com.raghav.societycrave.entity.Food;
import com.raghav.societycrave.security.JwtAuthenticatedUser;
import com.raghav.societycrave.service.AuthService;
import com.raghav.societycrave.service.FoodService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/food")
public class FoodController {

    private final FoodService foodService;
    private final AuthService authService;

    public FoodController(FoodService foodService, AuthService authService) {
        this.foodService = foodService;
        this.authService = authService;
    }

    @GetMapping
    public List<Food> getAllFoods(Authentication authentication) {
        return foodService.getAllFoodsForSociety(resolveAuthorizedSociety(authentication, null));
    }

    @GetMapping("/available")
    public List<Food> getAllAvailableFoods(Authentication authentication) {
        return foodService.getAvailableFoods(resolveAuthorizedSociety(authentication, null));
    }

    @GetMapping("/society/available")
    public List<Food> getAvailableFoodsBySociety(@RequestParam String societyName,
                                                 Authentication authentication) {
        return foodService.getAvailableFoods(resolveAuthorizedSociety(authentication, societyName));
    }

    @GetMapping("/society/chef")
    public List<Food> getFoodsByChefAndSociety(@RequestParam String chefName,
                                               @RequestParam String societyName,
                                               @RequestParam(required = false) String flatNumber,
                                               Authentication authentication) {
        String authorizedSociety = resolveAuthorizedSociety(authentication, societyName);
        if (flatNumber != null && !flatNumber.isBlank()) {
            return foodService.getFoodsByChefAndFlatAndSociety(chefName, flatNumber, authorizedSociety);
        }
        return foodService.getFoodsByChefAndSociety(chefName, authorizedSociety);
    }

    @GetMapping("/chef/{chefName}")
    public List<Food> getFoodsByChef(@PathVariable String chefName,
                                     Authentication authentication) {
        return foodService.getFoodsByChefAndSociety(chefName, resolveAuthorizedSociety(authentication, null));
    }

    @GetMapping("/chef/{chefName}/society/{societyName}")
    public List<Food> getFoodsByChefAndSocietyPath(@PathVariable String chefName,
                                                   @PathVariable String societyName,
                                                   Authentication authentication) {
        return foodService.getFoodsByChefAndSociety(
                chefName,
                resolveAuthorizedSociety(authentication, societyName)
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Food createFood(@RequestBody Food food, Authentication authentication) {
        JwtAuthenticatedUser principal = requireAuthenticatedUser(authentication);
        requireChef(principal);
        validateRequestedSociety(food.getSocietyName(), principal.societyName());
        return foodService.createFoodForProfile(food, resolveCurrentProfile(principal));
    }

    @PutMapping("/{id}")
    public Food updateFood(@PathVariable Long id,
                           @RequestBody Food food,
                           Authentication authentication) {
        JwtAuthenticatedUser principal = requireAuthenticatedUser(authentication);
        requireChef(principal);
        return foodService.updateFoodForProfile(id, food, resolveCurrentProfile(principal));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFood(@PathVariable Long id, Authentication authentication) {
        JwtAuthenticatedUser principal = requireAuthenticatedUser(authentication);
        requireChef(principal);
        foodService.deleteFoodForProfile(id, resolveCurrentProfile(principal));
    }

    private String resolveAuthorizedSociety(Authentication authentication, String requestedSociety) {
        JwtAuthenticatedUser principal = requireAuthenticatedUser(authentication);
        String authenticatedSociety = normalize(principal.societyName());
        if (authenticatedSociety == null || authenticatedSociety.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session is missing society scope.");
        }

        String requested = normalize(requestedSociety);
        if (requested != null && !requested.equalsIgnoreCase(authenticatedSociety)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cross-society food access is not allowed.");
        }
        return authenticatedSociety;
    }

    private JwtAuthenticatedUser requireAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtAuthenticatedUser principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid JWT.");
        }
        return principal;
    }

    private AuthResponse resolveCurrentProfile(JwtAuthenticatedUser principal) {
        return authService.getCurrentProfile(
                principal.subject(),
                principal.role(),
                principal.societyName()
        );
    }

    private void requireChef(JwtAuthenticatedUser principal) {
        if (!principal.isChef()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only chefs can mutate food.");
        }
    }

    private void validateRequestedSociety(String requestedSociety, String authenticatedSociety) {
        String requested = normalize(requestedSociety);
        String authenticated = normalize(authenticatedSociety);
        if (authenticated == null || authenticated.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session is missing society scope.");
        }
        if (requested != null && !requested.equalsIgnoreCase(authenticated)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cross-society food mutation is not allowed.");
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
