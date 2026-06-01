package com.raghav.societycrave.repository;

import com.raghav.societycrave.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    List<Complaint> findByStatusIgnoreCase(String status);

    List<Complaint> findBySocietyNameIgnoreCase(String societyName);

    List<Complaint> findBySocietyNameIgnoreCaseAndResidentEmailIgnoreCase(String societyName, String residentEmail);

    List<Complaint> findByFlatNumber(String flatNumber);

    List<Complaint> findByStatusIgnoreCaseAndFlatNumber(String status, String flatNumber);

    List<Complaint> findByStatusIgnoreCaseAndSocietyNameIgnoreCase(String status, String societyName);

    List<Complaint> findByStatusIgnoreCaseAndSocietyNameIgnoreCaseAndResidentEmailIgnoreCase(
            String status,
            String societyName,
            String residentEmail
    );

    List<Complaint> findByStatusIgnoreCaseOrderByCreatedAtDesc(String status);

    java.util.Optional<Complaint> findByIdAndSocietyNameIgnoreCase(Long id, String societyName);

    java.util.Optional<Complaint> findByIdAndSocietyNameIgnoreCaseAndResidentEmailIgnoreCase(Long id, String societyName, String residentEmail);

    Page<Complaint> findByStatusIgnoreCase(String status, Pageable pageable);
}
