package com.raghav.societycrave.dto.auth;

public record AuthResponse(
        String role,
        String displayName,
        String email,
        String chefCode,
        String flatNumber,
        String societyName,
        String chefCuisine
) {
}
