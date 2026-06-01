package com.raghav.societycrave.service;

import com.raghav.societycrave.dto.auth.AuthResponse;
import com.raghav.societycrave.entity.Food;
import com.raghav.societycrave.repository.FoodRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
public class FoodService {

    private final FoodRepository foodRepository;
    private final Validator validator;

    public FoodService(FoodRepository foodRepository, Validator validator) {
        this.foodRepository = foodRepository;
        this.validator = validator;
    }
   

    // Get all foods
    public List<Food> getAllFoodsForSociety(String societyName) {
        return foodRepository.findBySocietyNameIgnoreCaseOrderByChefNameAsc(societyName);
    }

    // Get available foods for a society
    public List<Food> getAvailableFoods(String societyName) {
        return foodRepository.findByAvailableTrueAndSocietyNameIgnoreCase(societyName);
    }
    public List<Food> getAvailableFoods() {
    return foodRepository.findByAvailableTrueOrderByChefNameAsc();
  }

    // Get foods by chef
    public List<Food> getFoodsByChef(String chefName) {
        return foodRepository.findByChefNameIgnoreCase(chefName);
    }

    // Get foods by chef & society
    public List<Food> getFoodsByChefAndSociety(String chefName, String societyName) {
        return foodRepository.findByChefNameIgnoreCaseAndSocietyNameIgnoreCase(chefName, societyName);
    }

    // Get foods by chef + flat & society
    public List<Food> getFoodsByChefAndFlatAndSociety(String chefName, String chefFlatNumber, String societyName) {
        return foodRepository.findByChefNameIgnoreCaseAndChefFlatNumberIgnoreCaseAndSocietyNameIgnoreCase(
                chefName, chefFlatNumber, societyName
        );
    }

    // Save or update food
    public Food saveFood(Food food) {
        normalizeFood(food);
        return foodRepository.save(food);
    }

    public Food createFoodForProfile(Food food, AuthResponse profile) {
        applyTrustedChefScope(food, profile);
        normalizeFood(food);
        validateFood(food);
        return foodRepository.save(food);
    }

    public Food updateFoodForProfile(Long id, Food updates, AuthResponse profile) {
        Food existing = getFoodById(id);
        ensureFoodBelongsToProfile(existing, profile);

        Food trusted = new Food();
        trusted.setId(existing.getId());
        trusted.setName(updates.getName());
        trusted.setDescription(updates.getDescription());
        trusted.setImageUrl(updates.getImageUrl());
        trusted.setPrice(updates.getPrice());
        trusted.setCategory(updates.getCategory());
        trusted.setAvailableDays(updates.getAvailableDays());
        trusted.setOpeningTime(updates.getOpeningTime());
        trusted.setClosingTime(updates.getClosingTime());
        trusted.setSlidingWindowMinutes(updates.getSlidingWindowMinutes());
        trusted.setAvailable(updates.isAvailable());

        // Keep chef/society ownership trusted, not body-controlled.
        trusted.setChefName(existing.getChefName());
        trusted.setChefFlatNumber(existing.getChefFlatNumber());
        trusted.setSocietyName(existing.getSocietyName());
        trusted.setChefCuisine(resolveTrustedCuisine(profile, existing.getChefCuisine()));

        normalizeFood(trusted);
        validateFood(trusted);
        return foodRepository.save(trusted);
    }

    public void deleteFoodForProfile(Long id, AuthResponse profile) {
        Food existing = getFoodById(id);
        ensureFoodBelongsToProfile(existing, profile);
        foodRepository.delete(existing);
    }

    // Delete food
    public void deleteFood(Long id) {
        foodRepository.deleteById(id);
    }

    private Food getFoodById(Long id) {
        return foodRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Food not found with id " + id));
    }

    private void ensureFoodBelongsToProfile(Food food, AuthResponse profile) {
        String profileSociety = normalize(profile.societyName());
        String profileName = normalize(profile.displayName());
        String profileFlat = normalize(profile.flatNumber());

        if (!normalize(food.getSocietyName()).equalsIgnoreCase(profileSociety)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cross-society food mutation is not allowed.");
        }
        if (!normalize(food.getChefName()).equalsIgnoreCase(profileName)
                || !normalize(food.getChefFlatNumber()).equalsIgnoreCase(profileFlat)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Food can only be mutated by its own chef identity.");
        }
    }

    private void applyTrustedChefScope(Food food, AuthResponse profile) {
        food.setChefName(profile.displayName());
        food.setChefFlatNumber(profile.flatNumber());
        food.setSocietyName(profile.societyName());
        food.setChefCuisine(resolveTrustedCuisine(profile, food.getChefCuisine()));
    }

    private String resolveTrustedCuisine(AuthResponse profile, String fallbackCuisine) {
        String trustedCuisine = normalize(profile.chefCuisine());
        if (trustedCuisine.isBlank()) {
            return normalize(fallbackCuisine);
        }
        return trustedCuisine;
    }

    private void normalizeFood(Food food) {
        if (food.getName() != null) food.setName(food.getName().trim());
        if (food.getChefName() != null) food.setChefName(food.getChefName().trim());
        if (food.getChefFlatNumber() != null) food.setChefFlatNumber(food.getChefFlatNumber().trim());
        if (food.getChefCuisine() != null) food.setChefCuisine(food.getChefCuisine().trim());
        if (food.getSocietyName() != null) food.setSocietyName(food.getSocietyName().trim());
        if (food.getCategory() != null) food.setCategory(food.getCategory().trim());
        if (food.getAvailableDays() != null) food.setAvailableDays(food.getAvailableDays().trim());
        if (food.getOpeningTime() != null) food.setOpeningTime(food.getOpeningTime().trim());
        if (food.getClosingTime() != null) food.setClosingTime(food.getClosingTime().trim());
        if (food.getDescription() != null) food.setDescription(food.getDescription().trim());
        if (food.getImageUrl() != null) food.setImageUrl(food.getImageUrl().trim());
        if (food.getSlidingWindowMinutes() != null && food.getSlidingWindowMinutes() <= 0) {
            food.setSlidingWindowMinutes(null);
        }
    }

    private void validateFood(Food food) {
        Set<ConstraintViolation<Food>> violations = validator.validate(food);
        if (!violations.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    violations.iterator().next().getMessage()
            );
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
