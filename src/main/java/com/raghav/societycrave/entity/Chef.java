package com.raghav.societycrave.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(
    name = "chef",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"flatNumber", "societyName"}),
        @UniqueConstraint(columnNames = {"chefCode"})
    }
)
public class Chef {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Chef name is mandatory")
    @Column(nullable = false)
    private String chefName;

    @Column(nullable = false, unique = true)
    private String chefCode;

    @NotBlank(message = "Email is mandatory")
    @Email(message = "Email should be valid")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Cuisine specialty is mandatory")
    @Column(nullable = false)
    private String chefCuisine;

    @NotBlank(message = "Flat number is mandatory")
    @Column(nullable = false)
    private String flatNumber;

    @NotBlank(message = "Society name is mandatory")
    @Column(nullable = false)
    private String societyName;

    @JsonIgnore
    @Column
    private String passwordHash;

    // ----------------------
    // Constructors
    // ----------------------
    public Chef() {}

    public Chef(String chefName, String chefCode, String email, String chefCuisine, String flatNumber, String societyName, String passwordHash) {
        this.chefName = chefName;
        this.chefCode = chefCode;
        this.email = email;
        this.chefCuisine = chefCuisine;
        this.flatNumber = flatNumber;
        this.societyName = societyName;
        this.passwordHash = passwordHash;
    }

    // ----------------------
    // Getters & Setters
    // ----------------------
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChefName() {
        return chefName;
    }

    public void setChefName(String chefName) {
        this.chefName = chefName;
    }

    public String getChefCode() {
        return chefCode;
    }

    public void setChefCode(String chefCode) {
        this.chefCode = chefCode;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getChefCuisine() {
        return chefCuisine;
    }

    public void setChefCuisine(String chefCuisine) {
        this.chefCuisine = chefCuisine;
    }

    public String getFlatNumber() {
        return flatNumber;
    }

    public void setFlatNumber(String flatNumber) {
        this.flatNumber = flatNumber;
    }

    public String getSocietyName() {
        return societyName;
    }

    public void setSocietyName(String societyName) {
        this.societyName = societyName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
