package com.raghav.societycrave.controller;

import com.raghav.societycrave.entity.Complaint;
import com.raghav.societycrave.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(final ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @GetMapping
    public List<Complaint> getAllComplaints() {
        return complaintService.getAllComplaints();
    }

    @GetMapping("/status")
    public List<Complaint> getComplaintsByStatus(@RequestParam final String status) {
        return complaintService.getComplaintsByStatus(status);
    }

    @GetMapping("/{id}")
    public Complaint getComplaintById(@PathVariable final Long id) {
        return complaintService.getComplaintById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Complaint createComplaint(@Valid @RequestBody final Complaint complaint) {
        return complaintService.saveComplaint(complaint);
    }

    @PutMapping("/{id}")
    public Complaint updateComplaint(@PathVariable final Long id,
                                     @Valid @RequestBody final Complaint complaint) {
        return complaintService.updateComplaint(id, complaint);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComplaint(@PathVariable final Long id) {
        complaintService.deleteComplaint(id);
    }
}
