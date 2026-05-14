package com.raghav.societycrave.controller;

import com.raghav.societycrave.entity.Food;
import com.raghav.societycrave.service.FoodService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/food")
public class FoodController {

    private final FoodService foodService;

    public FoodController(FoodService foodService) {
        this.foodService = foodService;
    }

    @GetMapping
    public List<Food> getAllFoods() {
        return foodService.getAllFoods();
    }

    @GetMapping("/available")
    public List<Food> getAllAvailableFoods() {
        return foodService.getAvailableFoods();
    }

    @GetMapping("/society/available")
    public List<Food> getAvailableFoodsBySociety(@RequestParam String societyName) {
        return foodService.getAvailableFoods(societyName);
    }

    @GetMapping("/society/chef")
    public List<Food> getFoodsByChefAndSociety(@RequestParam String chefName,
                                               @RequestParam String societyName,
                                               @RequestParam(required = false) String flatNumber) {
        if (flatNumber != null && !flatNumber.isBlank()) {
            return foodService.getFoodsByChefAndFlatAndSociety(chefName, flatNumber, societyName);
        }
        return foodService.getFoodsByChefAndSociety(chefName, societyName);
    }

    @GetMapping("/chef/{chefName}")
    public List<Food> getFoodsByChef(@PathVariable String chefName) {
        return foodService.getFoodsByChef(chefName);
    }

    @GetMapping("/chef/{chefName}/society/{societyName}")
    public List<Food> getFoodsByChefAndSocietyPath(@PathVariable String chefName, @PathVariable String societyName) {
        return foodService.getFoodsByChefAndSociety(chefName, societyName);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Food createFood(@Valid @RequestBody Food food) {
        return foodService.saveFood(food);
    }

    @PutMapping("/{id}")
    public Food updateFood(@PathVariable Long id, @Valid @RequestBody Food food) {
        food.setId(id);
        return foodService.saveFood(food);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFood(@PathVariable Long id) {
        foodService.deleteFood(id);
    }
}
