package com.raghav.societycrave.controller;

import com.raghav.societycrave.dto.auth.AuthResponse;
import com.raghav.societycrave.dto.auth.ChefLoginRequest;
import com.raghav.societycrave.dto.auth.ChefRegisterRequest;
import com.raghav.societycrave.dto.auth.CustomerLoginRequest;
import com.raghav.societycrave.dto.auth.CustomerRegisterRequest;
import com.raghav.societycrave.security.JwtAuthenticatedUser;
import com.raghav.societycrave.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/customers/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerCustomer(@Valid @RequestBody CustomerRegisterRequest request) {
        return authService.registerCustomer(request);
    }

    @PostMapping("/customers/login")
    public AuthResponse loginCustomer(@Valid @RequestBody CustomerLoginRequest request) {
        return authService.loginCustomer(request);
    }

    @PostMapping("/chefs/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerChef(@Valid @RequestBody ChefRegisterRequest request) {
        return authService.registerChef(request);
    }

    @PostMapping("/chefs/login")
    public AuthResponse loginChef(@Valid @RequestBody ChefLoginRequest request) {
        return authService.loginChef(request);
    }

    @GetMapping("/me")
    public AuthResponse me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtAuthenticatedUser principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid JWT.");
        }

        return authService.getCurrentProfile(
                principal.subject(),
                principal.role(),
                principal.societyName()
        );
    }
}
