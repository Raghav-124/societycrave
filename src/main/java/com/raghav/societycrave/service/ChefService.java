package com.raghav.societycrave.service;

import com.raghav.societycrave.entity.Chef;
import com.raghav.societycrave.repository.ChefRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ChefService {

    private final ChefRepository chefRepository;

    public ChefService(ChefRepository chefRepository) {
        this.chefRepository = chefRepository;
    }

    // ----------------------
    // Save a new chef
    // ----------------------
    public Chef saveChef(Chef chef) {
        normalizeChef(chef);

        // Check if a chef from the same flat already exists in this society
        chefRepository.findByChefNameIgnoreCaseAndSocietyName(chef.getChefName(), chef.getSocietyName())
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "A chef is already registered from this flat in the selected society.");
                });

        return chefRepository.save(chef);
    }

    // ----------------------
    // Get all chefs in a society
    // ----------------------
    public List<Chef> getChefsBySociety(String societyName) {
        return chefRepository.findBySocietyName(societyName);
    }

    // ----------------------
    // Helper method to normalize input strings
    // ----------------------
    private void normalizeChef(Chef chef) {
        if (chef.getChefName() != null) {
            chef.setChefName(chef.getChefName().trim());
        }
        if (chef.getChefCuisine() != null) {
            chef.setChefCuisine(chef.getChefCuisine().trim());
        }
        if (chef.getSocietyName() != null) {
            chef.setSocietyName(chef.getSocietyName().trim());
        }
    }
}