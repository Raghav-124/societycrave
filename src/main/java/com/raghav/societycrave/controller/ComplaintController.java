package com.raghav.societycrave.controller;

import com.raghav.societycrave.entity.Complaint;
import com.raghav.societycrave.security.JwtAuthenticatedUser;
import com.raghav.societycrave.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(final ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @GetMapping
    public List<Complaint> getAllComplaints(Authentication authentication) {
        JwtAuthenticatedUser principal = requireAuthenticatedUser(authentication);
        if (principal.isCustomer()) {
            return complaintService.getAllComplaintsForCustomer(principal.societyName(), requireResidentEmail(principal));
        }
        if (principal.isChef()) {
            return complaintService.getAllComplaintsForSociety(principal.societyName());
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unsupported role for complaint access.");
    }

    @GetMapping("/status")
    public List<Complaint> getComplaintsByStatus(@RequestParam final String status, Authentication authentication) {
        JwtAuthenticatedUser principal = requireAuthenticatedUser(authentication);
        if (principal.isCustomer()) {
            return complaintService.getComplaintsByStatusForCustomer(status, principal.societyName(), requireResidentEmail(principal));
        }
        if (principal.isChef()) {
            return complaintService.getComplaintsByStatusForSociety(status, principal.societyName());
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unsupported role for complaint access.");
    }

    @GetMapping("/{id}")
    public Complaint getComplaintById(@PathVariable final Long id, Authentication authentication) {
        JwtAuthenticatedUser principal = requireAuthenticatedUser(authentication);
        if (principal.isCustomer()) {
            return complaintService.getComplaintByIdForCustomer(id, principal.societyName(), requireResidentEmail(principal));
        }
        if (principal.isChef()) {
            return complaintService.getComplaintByIdForSociety(id, principal.societyName());
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unsupported role for complaint access.");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Complaint createComplaint(@Valid @RequestBody final Complaint complaint, Authentication authentication) {
        JwtAuthenticatedUser principal = requireAuthenticatedUser(authentication);
        requireCustomer(principal, "Only customers can create complaints.");
        complaintService.validateRequestedSociety(complaint.getSocietyName(), principal.societyName());
        complaint.setResidentName(principal.displayName());
        complaint.setFlatNumber(principal.flatNumber());
        complaint.setResidentEmail(requireResidentEmail(principal));
        complaint.setSocietyName(principal.societyName());
        complaint.setStatus("OPEN");
        complaint.setAssignedTo(null);
        complaint.setCreatedAt(null);
        complaint.setResolvedAt(null);
        return complaintService.saveComplaint(complaint);
    }

    @PutMapping("/{id}")
    public Complaint updateComplaint(@PathVariable final Long id,
                                     @Valid @RequestBody final Complaint complaint,
                                     Authentication authentication) {
        JwtAuthenticatedUser principal = requireAuthenticatedUser(authentication);
        if (principal.isCustomer()) {
            return complaintService.updateComplaintForCustomer(
                    id,
                    complaint,
                    principal.societyName(),
                    requireResidentEmail(principal)
            );
        }
        if (principal.isChef()) {
            return complaintService.updateComplaintForChef(id, complaint, principal.societyName());
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unsupported role for complaint updates.");
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComplaint(@PathVariable final Long id, Authentication authentication) {
        JwtAuthenticatedUser principal = requireAuthenticatedUser(authentication);
        requireCustomer(principal, "Only customers can delete complaints.");
        complaintService.deleteComplaintForCustomer(id, principal.societyName(), requireResidentEmail(principal));
    }

    private JwtAuthenticatedUser requireAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtAuthenticatedUser principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid JWT.");
        }
        return principal;
    }

    private String requireResidentEmail(JwtAuthenticatedUser principal) {
        String email = normalize(principal.email());
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Resident session is missing email identity.");
        }
        return email;
    }

    private void requireCustomer(JwtAuthenticatedUser principal, String message) {
        if (!principal.isCustomer()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
