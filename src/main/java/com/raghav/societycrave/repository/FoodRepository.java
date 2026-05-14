package com.raghav.societycrave.repository;

import com.raghav.societycrave.entity.Food;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FoodRepository extends JpaRepository<Food, Long> {

    // Get all available foods, ordered by chef name
    List<Food> findByAvailableTrueOrderByChefNameAsc();

    // Get all foods by a specific chef (case-insensitive)
    List<Food> findByChefNameIgnoreCase(String chefName);

    // Get all available foods in a society
    List<Food> findByAvailableTrueAndSocietyNameIgnoreCase(String societyName);

    // Get all foods by chef in a society (case-insensitive)
    List<Food> findByChefNameIgnoreCaseAndSocietyNameIgnoreCase(String chefName, String societyName);

    // Get all foods by chef + flat in a society (case-insensitive)
    List<Food> findByChefNameIgnoreCaseAndChefFlatNumberIgnoreCaseAndSocietyNameIgnoreCase(
            String chefName, String chefFlatNumber, String societyName);
}
