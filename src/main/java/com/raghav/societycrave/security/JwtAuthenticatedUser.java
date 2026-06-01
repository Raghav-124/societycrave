package com.raghav.societycrave.security;

public record JwtAuthenticatedUser(
        String subject,
        String role,
        String societyName
) {
}
