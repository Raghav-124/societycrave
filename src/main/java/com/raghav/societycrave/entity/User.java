package com.raghav.societycrave.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // optional: automatic ID generation
    private Long id;

    @NotBlank(message = "Name is mandatory")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Email is mandatory")
    @Email(message = "Email should be valid")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Flat number is mandatory")
    @Column(nullable = false)
    private String flatNumber;

    @NotBlank(message = "Society name is mandatory")
    @Column(nullable = false)
    private String societyName;

    @JsonIgnore
    @Column
    private String passwordHash;

    // Default constructor
    public User() {}

    // Constructor with fields
    public User(Long id, String name, String email, String flatNumber, String societyName, String passwordHash) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.flatNumber = flatNumber;
        this.societyName = societyName;
        this.passwordHash = passwordHash;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFlatNumber() { return flatNumber; }
    public void setFlatNumber(String flatNumber) { this.flatNumber = flatNumber; }

    public String getSocietyName() { return societyName; }
    public void setSocietyName(String societyName) { this.societyName = societyName; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}
