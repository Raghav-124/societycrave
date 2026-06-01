package com.raghav.societycrave.service;

import com.raghav.societycrave.entity.Food;
import com.raghav.societycrave.repository.FoodRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FoodService {

    private final FoodRepository foodRepository;

    public FoodService(FoodRepository foodRepository) {
        this.foodRepository = foodRepository;
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
        if (food.getName() != null) food.setName(food.getName().trim());
        if (food.getChefName() != null) food.setChefName(food.getChefName().trim());
        if (food.getChefFlatNumber() != null) food.setChefFlatNumber(food.getChefFlatNumber().trim());
        if (food.getChefCuisine() != null) food.setChefCuisine(food.getChefCuisine().trim());
        if (food.getSocietyName() != null) food.setSocietyName(food.getSocietyName().trim());
        if (food.getCategory() != null) food.setCategory(food.getCategory().trim());
        if (food.getAvailableDays() != null) food.setAvailableDays(food.getAvailableDays().trim());
        if (food.getOpeningTime() != null) food.setOpeningTime(food.getOpeningTime().trim());
        if (food.getClosingTime() != null) food.setClosingTime(food.getClosingTime().trim());
        if (food.getSlidingWindowMinutes() != null && food.getSlidingWindowMinutes() <= 0) {
            food.setSlidingWindowMinutes(null);
        }
        return foodRepository.save(food);
    }

    // Delete food
    public void deleteFood(Long id) {
        foodRepository.deleteById(id);
    }
}
