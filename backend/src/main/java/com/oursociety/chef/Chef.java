package com.oursociety.chef;

import com.oursociety.society.Society;
import com.oursociety.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "societycrave_chefs")
public class Chef {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @OneToOne(optional=false) @JoinColumn(name="user_id", nullable=false, unique=true) private User user;
    @ManyToOne(optional=false) @JoinColumn(name="society_id", nullable=false) private Society society;
    @Column(name="display_name", nullable=false) private String displayName;
    @Column(name="profile_photo_url") private String profilePhotoUrl;
    @Column(nullable=false, length=2000) private String bio;
    @Column(name="cuisine_specialties", nullable=false, length=1000) private String cuisineSpecialties;
    @Column(name="food_categories", nullable=false, length=1000) private String foodCategories;
    @Column(name="pickup_instructions", nullable=false, length=1000) private String pickupInstructions;
    @Column(nullable=false) private Instant createdAt = Instant.now();
    protected Chef() {}
    public Chef(User user, Society society, String displayName, String profilePhotoUrl, String bio, String cuisineSpecialties, String foodCategories, String pickupInstructions) {
        this.user=user; this.society=society; this.displayName=displayName; this.profilePhotoUrl=profilePhotoUrl;
        this.bio=bio; this.cuisineSpecialties=cuisineSpecialties; this.foodCategories=foodCategories; this.pickupInstructions=pickupInstructions;
    }
    public UUID getId(){return id;} public User getUser(){return user;} public Society getSociety(){return society;}
    public String getDisplayName(){return displayName;} public String getProfilePhotoUrl(){return profilePhotoUrl;}
    public String getBio(){return bio;} public String getCuisineSpecialties(){return cuisineSpecialties;}
    public String getFoodCategories(){return foodCategories;} public String getPickupInstructions(){return pickupInstructions;}
}
