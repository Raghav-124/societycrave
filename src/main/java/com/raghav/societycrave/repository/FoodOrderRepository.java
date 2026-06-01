package com.raghav.societycrave.repository;

import com.raghav.societycrave.entity.FoodOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface FoodOrderRepository extends JpaRepository<FoodOrder, Long> {

    // Find orders by status (PLACED, ACCEPTED, DELIVERED)
    List<FoodOrder> findByStatusIgnoreCase(String status);

    List<FoodOrder> findBySocietyNameIgnoreCase(String societyName);

    List<FoodOrder> findByStatusIgnoreCaseAndSocietyNameIgnoreCase(String status, String societyName);

    Optional<FoodOrder> findByIdAndSocietyNameIgnoreCase(Long id, String societyName);

    // Find orders by customer name
    List<FoodOrder> findByCustomerNameIgnoreCase(String customerName);

    // Find orders by flat number (resident)
    List<FoodOrder> findByFlatNumber(String flatNumber);

    // Find orders by flat number and status
    List<FoodOrder> findByFlatNumberAndStatusIgnoreCase(String flatNumber, String status);

    // Support pagination for flat number
    Page<FoodOrder> findByFlatNumber(String flatNumber, Pageable pageable);
}
