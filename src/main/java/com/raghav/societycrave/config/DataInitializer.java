package com.raghav.societycrave.config;

import com.raghav.societycrave.entity.Chef;
import com.raghav.societycrave.repository.ChefRepository;
import com.raghav.societycrave.entity.User;
import com.raghav.societycrave.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Profile("dev")
@ConditionalOnProperty(prefix = "app", name = "seed-demo-data", havingValue = "true")
@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedUsers(UserRepository userRepository,
                                ChefRepository chefRepository,
                                PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByEmailIgnoreCase("raghav@example.com").isPresent()) {
                seedChefIfMissing(chefRepository, passwordEncoder);
                return;
            }

            User user = new User();
            user.setName("Raghav Agrawal");
            user.setEmail("raghav@example.com");
            user.setFlatNumber("A-101");
            user.setSocietyName("Green Valley Residency");
            user.setPasswordHash(passwordEncoder.encode("Society123"));
            userRepository.save(user);

            seedChefIfMissing(chefRepository, passwordEncoder);
        };
    }

    private void seedChefIfMissing(ChefRepository chefRepository, PasswordEncoder passwordEncoder) {
        if (chefRepository.existsByEmailIgnoreCase("chef.meera@example.com")) {
            return;
        }

        Chef chef = new Chef();
        chef.setChefName("Meera Joshi");
        chef.setChefCode("CHEF-MEERA01");
        chef.setEmail("chef.meera@example.com");
        chef.setChefCuisine("North Indian");
        chef.setFlatNumber("B-204");
        chef.setSocietyName("Green Valley Residency");
        chef.setPasswordHash(passwordEncoder.encode("Society123"));
        chefRepository.save(chef);
    }
}
