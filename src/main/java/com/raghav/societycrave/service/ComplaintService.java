package com.raghav.societycrave.service;

import com.raghav.societycrave.entity.Complaint;
import com.raghav.societycrave.repository.ComplaintRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

    // ----------------------
    // Get complaint by ID
    // ----------------------
    public Complaint getComplaintById(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Complaint not found with id " + id));
    }

    // ----------------------
    // Save new complaint
    // ----------------------
    public Complaint saveComplaint(Complaint complaint) {
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

    // ----------------------
    // Delete complaint by ID
    // ----------------------
    public void deleteComplaint(Long id) {
        if (!complaintRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Complaint not found with id " + id);
        }
        complaintRepository.deleteById(id);
    }

    // ----------------------
    // Get complaints by status
    // ----------------------
    public List<Complaint> getComplaintsByStatus(String status) {
        return complaintRepository.findByStatusIgnoreCase(status);
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
        if (complaint.getStatus() != null) {
            complaint.setStatus(complaint.getStatus().trim().toUpperCase());
        }
        if (complaint.getAssignedTo() != null) {
            complaint.setAssignedTo(complaint.getAssignedTo().trim());
        }
    }
}