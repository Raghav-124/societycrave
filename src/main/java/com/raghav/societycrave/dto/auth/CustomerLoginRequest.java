package com.raghav.societycrave.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CustomerLoginRequest(
        @NotBlank(message = "Email is mandatory") @Email(message = "Email should be valid") String email,
        @NotBlank(message = "Password is mandatory") String password,
        @NotBlank(message = "Society name is mandatory") String societyName
) {
}
