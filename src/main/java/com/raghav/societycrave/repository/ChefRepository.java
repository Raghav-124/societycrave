package com.raghav.societycrave.repository;

import com.raghav.societycrave.entity.Chef;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChefRepository extends JpaRepository<Chef, Long> {

    Optional<Chef> findByChefNameIgnoreCaseAndSocietyName(String chefName, String societyName);

    List<Chef> findBySocietyName(String societyName);

    List<Chef> findBySocietyNameOrderByChefNameAsc(String societyName);

    Optional<Chef> findByFlatNumberIgnoreCaseAndSocietyNameIgnoreCase(String flatNumber, String societyName);

    Optional<Chef> findByChefCodeIgnoreCaseAndSocietyNameIgnoreCase(String chefCode, String societyName);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByChefCode(String chefCode);
}
