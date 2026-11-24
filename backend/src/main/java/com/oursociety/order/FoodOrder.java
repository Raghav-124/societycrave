package com.oursociety.order;
import com.oursociety.chef.Chef; import com.oursociety.listing.Listing; import com.oursociety.society.Society; import com.oursociety.user.User;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="societycrave_orders")
public class FoodOrder {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @ManyToOne(optional=false) @JoinColumn(name="resident_user_id",nullable=false) private User resident;
 @ManyToOne(optional=false) @JoinColumn(name="chef_id",nullable=false) private Chef chef;
 @ManyToOne(optional=false) @JoinColumn(name="society_id",nullable=false) private Society society;
 @ManyToOne(optional=false) @JoinColumn(name="listing_id",nullable=false) private Listing listing;
 @Column(nullable=false) private int quantity;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private Status status=Status.PLACED;
 @Column(nullable=false) private Instant createdAt=Instant.now();
 protected FoodOrder(){}
 public FoodOrder(User resident,Listing listing,int quantity){this.resident=resident;this.chef=listing.getChef();this.society=listing.getSociety();this.listing=listing;this.quantity=quantity;}
 public UUID getId(){return id;} public User getResident(){return resident;} public Chef getChef(){return chef;} public Society getSociety(){return society;} public Listing getListing(){return listing;} public int getQuantity(){return quantity;} public Status getStatus(){return status;}
 public enum Status { PLACED, CANCELLED, FULFILLED }
}
