package com.raghav.societycrave.service;

import com.raghav.societycrave.entity.Complaint;
import com.raghav.societycrave.repository.ComplaintRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ComplaintService {

    private final ComplaintRepository complaintRepository;

    public ComplaintService(ComplaintRepository complaintRepository) {
        this.complaintRepository = complaintRepository;
    }

    // ----------------------
    // Get all complaints
    // ----------------------
    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAll();
    }

    public List<Complaint> getAllComplaintsForSociety(String societyName) {
        return complaintRepository.findBySocietyNameIgnoreCase(requireSocietyScope(societyName));
    }

    public List<Complaint> getAllComplaintsForCustomer(String societyName, String residentEmail) {
        return complaintRepository.findBySocietyNameIgnoreCaseAndResidentEmailIgnoreCase(
                requireSocietyScope(societyName),
                requireResidentEmail(residentEmail)
        );
    }

    // ----------------------
    // Get complaint by ID
    // ----------------------
    public Complaint getComplaintById(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Complaint not found with id " + id));
    }

    public Complaint getComplaintByIdForSociety(Long id, String societyName) {
        Complaint complaint = getComplaintById(id);
        if (!requireSocietyScope(societyName).equalsIgnoreCase(normalize(complaint.getSocietyName()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cross-society complaint access is not allowed.");
        }
        return complaint;
    }

    public Complaint getComplaintByIdForCustomer(Long id, String societyName, String residentEmail) {
        Complaint complaint = getComplaintByIdForSociety(id, societyName);
        enforceCustomerReadAccess(complaint, residentEmail);
        return complaint;
    }

    // ----------------------
    // Save new complaint
    // ----------------------
    public Complaint saveComplaint(Complaint complaint) {
        complaint.setCreatedAt(LocalDateTime.now());
        complaint.setResolvedAt(null);
        normalizeComplaint(complaint);
        return complaintRepository.save(complaint);
    }

    // ----------------------
    // Update existing complaint
    // ----------------------
    public Complaint updateComplaint(Long id, Complaint complaintDetails) {
        Complaint complaint = getComplaintById(id);
        normalizeComplaint(complaintDetails);

        complaint.setTitle(complaintDetails.getTitle());
        complaint.setDescription(complaintDetails.getDescription());
        complaint.setResidentName(complaintDetails.getResidentName());
        complaint.setFlatNumber(complaintDetails.getFlatNumber());
        complaint.setStatus(complaintDetails.getStatus());
        complaint.setAssignedTo(complaintDetails.getAssignedTo());
        complaint.setCreatedAt(complaintDetails.getCreatedAt());
        complaint.setResolvedAt(complaintDetails.getResolvedAt());

        return complaintRepository.save(complaint);
    }

    public Complaint updateComplaintForCustomer(Long id,
                                                Complaint complaintDetails,
                                                String societyName,
                                                String residentEmail) {
        Complaint complaint = getComplaintByIdForSociety(id, societyName);
        enforceCustomerOwnership(complaint, residentEmail);
        normalizeComplaint(complaintDetails);

        complaint.setTitle(complaintDetails.getTitle());
        complaint.setDescription(complaintDetails.getDescription());
        return complaintRepository.save(complaint);
    }

    public Complaint updateComplaintForChef(Long id, Complaint complaintDetails, String societyName) {
        Complaint complaint = getComplaintByIdForSociety(id, societyName);
        normalizeComplaint(complaintDetails);

        String targetStatus = complaintDetails.getStatus();
        if (targetStatus != null && !targetStatus.isBlank()) {
            complaint.setStatus(targetStatus);
            if (isResolvedStatus(targetStatus)) {
                complaint.setResolvedAt(LocalDateTime.now());
            } else {
                complaint.setResolvedAt(null);
            }
        }

        complaint.setAssignedTo(complaintDetails.getAssignedTo());
        return complaintRepository.save(complaint);
    }

    // ----------------------
    // Delete complaint by ID
    // ----------------------
    public void deleteComplaint(Long id) {
        if (!complaintRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Complaint not found with id " + id);
        }
        complaintRepository.deleteById(id);
    }

    public void deleteComplaintForCustomer(Long id, String societyName, String residentEmail) {
        Complaint complaint = getComplaintByIdForSociety(id, societyName);
        enforceCustomerOwnership(complaint, residentEmail);
        complaintRepository.delete(complaint);
    }

    // ----------------------
    // Get complaints by status
    // ----------------------
    public List<Complaint> getComplaintsByStatus(String status) {
        return complaintRepository.findByStatusIgnoreCase(status);
    }

    public List<Complaint> getComplaintsByStatusForSociety(String status, String societyName) {
        return complaintRepository.findByStatusIgnoreCaseAndSocietyNameIgnoreCase(
                status,
                requireSocietyScope(societyName)
        );
    }

    public List<Complaint> getComplaintsByStatusForCustomer(String status, String societyName, String residentEmail) {
        return complaintRepository.findByStatusIgnoreCaseAndSocietyNameIgnoreCaseAndResidentEmailIgnoreCase(
                status,
                requireSocietyScope(societyName),
                requireResidentEmail(residentEmail)
        );
    }

    public void validateRequestedSociety(String requestedSociety, String authenticatedSociety) {
        String requested = normalize(requestedSociety);
        String authenticated = normalize(authenticatedSociety);
        if (authenticated.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session is missing society scope.");
        }
        if (!requested.isBlank() && !requested.equalsIgnoreCase(authenticated)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cross-society complaint creation is not allowed.");
        }
    }

    // ----------------------
    // Helper method to normalize string fields
    // ----------------------
    private void normalizeComplaint(Complaint complaint) {
        if (complaint.getTitle() != null) {
            complaint.setTitle(complaint.getTitle().trim());
        }
        if (complaint.getDescription() != null) {
            complaint.setDescription(complaint.getDescription().trim());
        }
        if (complaint.getResidentName() != null) {
            complaint.setResidentName(complaint.getResidentName().trim());
        }
        if (complaint.getFlatNumber() != null) {
            complaint.setFlatNumber(complaint.getFlatNumber().trim());
        }
        if (complaint.getResidentEmail() != null) {
            complaint.setResidentEmail(complaint.getResidentEmail().trim());
        }
        if (complaint.getSocietyName() != null) {
            complaint.setSocietyName(complaint.getSocietyName().trim());
        }
        if (complaint.getStatus() != null) {
            complaint.setStatus(complaint.getStatus().trim().toUpperCase());
        }
        if (complaint.getAssignedTo() != null) {
            complaint.setAssignedTo(complaint.getAssignedTo().trim());
        }
    }

    private void enforceCustomerOwnership(Complaint complaint, String residentEmail) {
        String complaintEmail = normalize(complaint.getResidentEmail());
        String principalEmail = requireResidentEmail(residentEmail);
        if (complaintEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Customers can only manage complaints with verified ownership.");
        }
        if (!complaintEmail.equalsIgnoreCase(principalEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Customers can only manage their own complaints.");
        }
    }

    private void enforceCustomerReadAccess(Complaint complaint, String residentEmail) {
        String complaintEmail = normalize(complaint.getResidentEmail());
        String principalEmail = requireResidentEmail(residentEmail);
        if (complaintEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Customers can only view complaints with verified ownership.");
        }
        if (!complaintEmail.equalsIgnoreCase(principalEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Customers can only view their own complaints.");
        }
    }

    private boolean isResolvedStatus(String status) {
        String normalized = normalize(status).toUpperCase();
        return normalized.equals("RESOLVED") || normalized.equals("CLOSED");
    }

    private String requireSocietyScope(String societyName) {
        String normalized = normalize(societyName);
        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session is missing society scope.");
        }
        return normalized;
    }

    private String requireResidentEmail(String residentEmail) {
        String normalized = normalize(residentEmail);
        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Resident session is missing email identity.");
        }
        return normalized;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
