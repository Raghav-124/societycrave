package com.oursociety.society;

import jakarta.persistence.*;
import java.util.UUID;

@Entity @Table(name="societycrave_societies", indexes=@Index(name="idx_sc_society_search", columnList="city,locality,name"))
public class Society {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @Column(nullable=false) private String name;
    @Column(nullable=false) private String locality;
    @Column(nullable=false) private String city;
    @Column(name="pin_code", nullable=false) private String pinCode;
    private Double latitude;
    private Double longitude;
    @Column(name="geohash", length=12) private String geohash;
    protected Society() {}
    public UUID getId(){return id;} public String getName(){return name;} public String getLocality(){return locality;} public String getCity(){return city;} public String getPinCode(){return pinCode;} public Double getLatitude(){return latitude;} public Double getLongitude(){return longitude;}
}
