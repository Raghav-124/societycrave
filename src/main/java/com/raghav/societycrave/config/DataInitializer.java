package com.raghav.societycrave.config;

import com.raghav.societycrave.entity.User;
import com.raghav.societycrave.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByEmailIgnoreCase("raghav@example.com").isPresent()) {
                return;
            }

            User user = new User();
            user.setName("Raghav Agrawal");
            user.setEmail("raghav@example.com");
            user.setFlatNumber("A-101");
            user.setSocietyName("Default Society");
            user.setPasswordHash(passwordEncoder.encode("Society123"));
            userRepository.save(user);
        };
    }
}
