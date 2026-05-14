package com.raghav.societycrave.repository;

import com.raghav.societycrave.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    // Find user by exact email (case-insensitive)
    Optional<User> findByEmailIgnoreCase(String email);

    List<User> findBySocietyNameIgnoreCase(String societyName);

    boolean existsByEmailIgnoreCase(String email);

    // Filter users by name OR email (case-insensitive) with pagination
    Page<User> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String name, String email, Pageable pageable);
}
