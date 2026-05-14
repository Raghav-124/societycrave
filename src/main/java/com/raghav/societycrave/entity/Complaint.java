package com.raghav.societycrave.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

@Entity
@Table(name = "complaint")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Complaint title is mandatory")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "Complaint description is mandatory")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    private String residentName;

    private String flatNumber;

    @Column(nullable = false)
    private String status = "OPEN"; // Default status

    private String assignedTo;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now(); // auto set on creation

    private LocalDateTime resolvedAt;

    // ----------------------
    // Constructors
    // ----------------------
    public Complaint() {}

    public Complaint(Long id, String title, String description, String residentName, String flatNumber,
                     String status, String assignedTo, LocalDateTime createdAt, LocalDateTime resolvedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.residentName = residentName;
        this.flatNumber = flatNumber;
        this.status = status;
        this.assignedTo = assignedTo;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.resolvedAt = resolvedAt;
    }

    // ----------------------
    // Getters & Setters
    // ----------------------
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getResidentName() { return residentName; }
    public void setResidentName(String residentName) { this.residentName = residentName; }

    public String getFlatNumber() { return flatNumber; }
    public void setFlatNumber(String flatNumber) { this.flatNumber = flatNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}