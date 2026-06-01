package com.raghav.societycrave.service;

import com.raghav.societycrave.dto.auth.AuthResponse;
import com.raghav.societycrave.dto.auth.ChefLoginRequest;
import com.raghav.societycrave.dto.auth.ChefRegisterRequest;
import com.raghav.societycrave.dto.auth.CustomerLoginRequest;
import com.raghav.societycrave.dto.auth.CustomerRegisterRequest;
import com.raghav.societycrave.entity.Chef;
import com.raghav.societycrave.entity.User;
import com.raghav.societycrave.repository.ChefRepository;
import com.raghav.societycrave.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final ChefRepository chefRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       ChefRepository chefRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.chefRepository = chefRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse registerCustomer(CustomerRegisterRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A customer with this email already exists.");
        }

        User user = new User();
        user.setName(normalize(request.name()));
        user.setEmail(email);
        user.setFlatNumber(normalize(request.flatNumber()));
        user.setSocietyName(normalize(request.societyName()));
        user.setPasswordHash(passwordEncoder.encode(validatePassword(request.password())));

        User savedUser = userRepository.save(user);
        return toCustomerResponse(savedUser, createTokenForCustomer(savedUser));
    }

    public AuthResponse loginCustomer(CustomerLoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalize(request.email()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid customer credentials."));

        if (user.getSocietyName() == null || !user.getSocietyName().equalsIgnoreCase(normalize(request.societyName()))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "This customer does not belong to the selected society.");
        }

        verifyPassword(user.getPasswordHash(), request.password(), "Invalid customer credentials.");
        return toCustomerResponse(user, createTokenForCustomer(user));
    }

    public AuthResponse registerChef(ChefRegisterRequest request) {
        String chefName = normalize(request.chefName());
        String email = normalize(request.email());
        String flatNumber = normalize(request.flatNumber());
        String societyName = normalize(request.societyName());
        String cuisine = normalize(request.chefCuisine());

        if (chefRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A chef with this email already exists.");
        }

        chefRepository.findByFlatNumberIgnoreCaseAndSocietyNameIgnoreCase(flatNumber, societyName)
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "This flat number is already used by another chef in this society."
                    );
                });

        Chef chef = new Chef();
        chef.setChefName(chefName);
        chef.setChefCode(generateChefCode());
        chef.setEmail(email);
        chef.setChefCuisine(cuisine);
        chef.setFlatNumber(flatNumber);
        chef.setSocietyName(societyName);
        chef.setPasswordHash(passwordEncoder.encode(validatePassword(request.password())));

        Chef savedChef = chefRepository.save(chef);
        return toChefResponse(savedChef, createTokenForChef(savedChef));
    }

    public AuthResponse loginChef(ChefLoginRequest request) {
        Chef chef = chefRepository.findByChefCodeIgnoreCaseAndSocietyNameIgnoreCase(
                        normalize(request.chefCode()),
                        normalize(request.societyName())
                )
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid chef credentials."));

        verifyPassword(chef.getPasswordHash(), request.password(), "Invalid chef credentials.");
        return toChefResponse(chef, createTokenForChef(chef));
    }

    public AuthResponse getCurrentProfile(String subject, String role, String societyName) {
        if ("Chef".equalsIgnoreCase(role)) {
            Chef chef = chefRepository.findByChefCodeIgnoreCaseAndSocietyNameIgnoreCase(subject, societyName)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chef session is invalid."));
            return toChefResponse(chef, null);
        }

        User user = userRepository.findByEmailIgnoreCase(subject)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Customer session is invalid."));

        if (user.getSocietyName() == null || !user.getSocietyName().equalsIgnoreCase(normalize(societyName))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Customer session is invalid.");
        }

        return toCustomerResponse(user, null);
    }

    private void verifyPassword(String passwordHash, String rawPassword, String message) {
        if (passwordHash == null || passwordHash.isBlank() || !passwordEncoder.matches(rawPassword, passwordHash)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
        }
    }

    private String validatePassword(String password) {
        String trimmedPassword = password == null ? "" : password.trim();
        if (trimmedPassword.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters long.");
        }
        boolean hasUpper = trimmedPassword.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = trimmedPassword.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = trimmedPassword.chars().anyMatch(Character::isDigit);
        if (!hasUpper || !hasLower || !hasDigit) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password must include at least one uppercase letter, one lowercase letter, and one number."
            );
        }
        return trimmedPassword;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private AuthResponse toCustomerResponse(User user, String accessToken) {
        return new AuthResponse(
                accessToken,
                "Bearer",
                "Customer",
                user.getName(),
                user.getEmail(),
                null,
                user.getFlatNumber(),
                user.getSocietyName(),
                null
        );
    }

    private AuthResponse toChefResponse(Chef chef, String accessToken) {
        return new AuthResponse(
                accessToken,
                "Bearer",
                "Chef",
                chef.getChefName(),
                chef.getEmail(),
                chef.getChefCode(),
                chef.getFlatNumber(),
                chef.getSocietyName(),
                chef.getChefCuisine()
        );
    }

    private String generateChefCode() {
        String chefCode;
        do {
            chefCode = "CHEF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (chefRepository.existsByChefCode(chefCode));
        return chefCode;
    }

    private String createTokenForCustomer(User user) {
        return jwtService.generateToken(user.getEmail(), "Customer", user.getSocietyName());
    }

    private String createTokenForChef(Chef chef) {
        return jwtService.generateToken(chef.getChefCode(), "Chef", chef.getSocietyName());
    }
}
