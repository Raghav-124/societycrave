package com.raghav.societycrave.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CustomerRegisterRequest(
        @NotBlank(message = "Name is mandatory") String name,
        @NotBlank(message = "Email is mandatory") @Email(message = "Email should be valid") String email,
        @NotBlank(message = "Flat number is mandatory") String flatNumber,
        @NotBlank(message = "Society name is mandatory") String societyName,
        @NotBlank(message = "Password is mandatory") String password
) {
}
