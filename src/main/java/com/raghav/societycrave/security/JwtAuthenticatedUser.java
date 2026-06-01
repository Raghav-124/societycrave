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
    public boolean isCustomer() {
        if (role == null) {
            return false;
        }
        String normalized = role.trim().toUpperCase();
        return normalized.equals("CUSTOMER") || normalized.equals("ROLE_CUSTOMER");
    }

    public boolean isChef() {
        if (role == null) {
            return false;
        }
        String normalized = role.trim().toUpperCase();
        return normalized.equals("CHEF") || normalized.equals("ROLE_CHEF");
    }
}
