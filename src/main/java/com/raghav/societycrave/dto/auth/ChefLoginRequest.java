package com.raghav.societycrave.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record ChefLoginRequest(
        @NotBlank(message = "Chef ID is mandatory") String chefCode,
        @NotBlank(message = "Society name is mandatory") String societyName,
        @NotBlank(message = "Password is mandatory") String password
) {
}
