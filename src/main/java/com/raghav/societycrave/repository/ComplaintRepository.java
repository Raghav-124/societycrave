package com.raghav.societycrave.repository;

import com.raghav.societycrave.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    List<Complaint> findByStatusIgnoreCase(String status);

    List<Complaint> findByFlatNumber(String flatNumber);

    List<Complaint> findByStatusIgnoreCaseAndFlatNumber(String status, String flatNumber);

    List<Complaint> findByStatusIgnoreCaseOrderByCreatedAtDesc(String status);

    Page<Complaint> findByStatusIgnoreCase(String status, Pageable pageable);
}