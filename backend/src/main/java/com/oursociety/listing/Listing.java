package com.oursociety.listing;

import com.oursociety.chef.Chef;
import com.oursociety.society.Society;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="societycrave_listings", indexes=@Index(name="idx_sc_listing_society_status", columnList="society_id,status"))
public class Listing {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(optional=false) @JoinColumn(name="chef_id",nullable=false) private Chef chef;
    @ManyToOne(optional=false) @JoinColumn(name="society_id",nullable=false) private Society society;
    @Column(nullable=false) private String title;
    @Column(nullable=false,length=2000) private String description;
    @Column(name="price_paise",nullable=false) private int pricePaise;
    @Column(name="pickup_instructions",length=1000) private String pickupInstructions;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Status status=Status.ACTIVE;
    @Column(nullable=false) private Instant createdAt=Instant.now();
    protected Listing(){}
    public Listing(Chef chef,String title,String description,int pricePaise,String pickupInstructions){
        this.chef=chef; this.society=chef.getSociety(); this.title=title; this.description=description;
        this.pricePaise=pricePaise; this.pickupInstructions=pickupInstructions;
    }
    public UUID getId(){return id;} public Chef getChef(){return chef;} public Society getSociety(){return society;} public String getTitle(){return title;} public String getDescription(){return description;} public int getPricePaise(){return pricePaise;} public String getPickupInstructions(){return pickupInstructions;} public Status getStatus(){return status;}
    public enum Status { ACTIVE, INACTIVE }
}
