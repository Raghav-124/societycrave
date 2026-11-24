package com.oursociety.chef;

import com.oursociety.society.Society;
import com.oursociety.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChefRepository extends JpaRepository<Chef, UUID> {
    Optional<Chef> findByUser(User user);
    List<Chef> findAllBySociety(Society society);
}
