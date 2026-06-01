package com.raghav.societycrave.security;

public record JwtAuthenticatedUser(
        String subject,
        String role,
        String displayName,
        String email,
        String chefCode,
        String flatNumber,
        String societyName,
        String chefCuisine
) {
}
