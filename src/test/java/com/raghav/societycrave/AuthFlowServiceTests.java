package com.raghav.societycrave;

import com.raghav.societycrave.dto.auth.AuthResponse;
import com.raghav.societycrave.dto.auth.ChefLoginRequest;
import com.raghav.societycrave.dto.auth.ChefRegisterRequest;
import com.raghav.societycrave.dto.auth.CustomerLoginRequest;
import com.raghav.societycrave.dto.auth.CustomerRegisterRequest;
import com.raghav.societycrave.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "DB_URL=jdbc:h2:mem:auth-flow-tests;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "DB_USERNAME=sa",
        "DB_PASSWORD=",
        "DB_DRIVER=org.h2.Driver",
        "DB_DIALECT=org.hibernate.dialect.H2Dialect"
})
class AuthFlowServiceTests {

    @Autowired
    private AuthService authService;

    @Test
    void customerRegisterAndLoginWork() {
        AuthResponse registered = authService.registerCustomer(
                new CustomerRegisterRequest(
                        "Resident One",
                        "resident.one@example.com",
                        "C-301",
                        "Green Valley Residency",
                        "Society123"
                )
        );

        assertThat(registered.accessToken()).isNotBlank();
        assertThat(registered.tokenType()).isEqualTo("Bearer");
        assertThat(registered.role()).isEqualTo("Customer");
        assertThat(registered.displayName()).isEqualTo("Resident One");

        AuthResponse loggedIn = authService.loginCustomer(
                new CustomerLoginRequest(
                        "resident.one@example.com",
                        "Society123",
                        "Green Valley Residency"
                )
        );

        assertThat(loggedIn.accessToken()).isNotBlank();
        assertThat(loggedIn.tokenType()).isEqualTo("Bearer");
        assertThat(loggedIn.email()).isEqualTo("resident.one@example.com");
        assertThat(loggedIn.societyName()).isEqualTo("Green Valley Residency");
    }

    @Test
    void chefRegisterAndLoginWork() {
        AuthResponse registered = authService.registerChef(
                new ChefRegisterRequest(
                        "Chef Test",
                        "chef.test@example.com",
                        "South Indian",
                        "D-402",
                        "Green Valley Residency",
                        "Society123"
                )
        );

        assertThat(registered.accessToken()).isNotBlank();
        assertThat(registered.tokenType()).isEqualTo("Bearer");
        assertThat(registered.role()).isEqualTo("Chef");
        assertThat(registered.chefCode()).startsWith("CHEF-");

        AuthResponse loggedIn = authService.loginChef(
                new ChefLoginRequest(
                        registered.chefCode(),
                        "Green Valley Residency",
                        "Society123"
                )
        );

        assertThat(loggedIn.accessToken()).isNotBlank();
        assertThat(loggedIn.tokenType()).isEqualTo("Bearer");
        assertThat(loggedIn.displayName()).isEqualTo("Chef Test");
        assertThat(loggedIn.chefCode()).isEqualTo(registered.chefCode());
    }
}
