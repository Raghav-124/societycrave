package com.raghav.societycrave.controller;

import com.raghav.societycrave.entity.Chef;
import com.raghav.societycrave.service.ChefService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chefs")
public class ChefController {

    private final ChefService chefService;

    public ChefController(ChefService chefService) {
        this.chefService = chefService;
    }

    @GetMapping("/society/{societyName}")
    public List<Chef> getChefsBySociety(@PathVariable String societyName) {
        return chefService.getChefsBySociety(societyName);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Chef createChef(@Valid @RequestBody Chef chef) {
        return chefService.saveChef(chef);
    }
}
