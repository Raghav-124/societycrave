package com.raghav.societycrave.repository;

import com.raghav.societycrave.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Get payments by status (case-insensitive)
    List<Payment> findByStatusIgnoreCase(String status);

    List<Payment> findBySocietyNameIgnoreCase(String societyName);

    List<Payment> findBySocietyNameIgnoreCaseAndResidentEmailIgnoreCase(String societyName, String residentEmail);

    List<Payment> findByStatusIgnoreCaseAndSocietyNameIgnoreCase(String status, String societyName);

    List<Payment> findByStatusIgnoreCaseAndSocietyNameIgnoreCaseAndResidentEmailIgnoreCase(
            String status,
            String societyName,
            String residentEmail
    );

    java.util.Optional<Payment> findByIdAndSocietyNameIgnoreCase(Long id, String societyName);

    java.util.Optional<Payment> findByIdAndSocietyNameIgnoreCaseAndResidentEmailIgnoreCase(
            Long id,
            String societyName,
            String residentEmail
    );

    // Get payments for a specific flat number (case-insensitive)
    List<Payment> findByFlatNumberIgnoreCase(String flatNumber);

    // Optional: get payments by resident name (case-insensitive)
    List<Payment> findByResidentNameIgnoreCase(String residentName);
}
