package com.oursociety.user;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="societycrave_users", uniqueConstraints=@UniqueConstraint(name="uk_sc_user_provider_subject", columnNames={"provider","provider_subject"}))
public class User {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @Column(nullable=false) private String provider;
    @Column(name="provider_subject", nullable=false) private String providerSubject;
    @Column(nullable=false) private String email;
    private String displayName;
    @Column(nullable=false) private Instant createdAt = Instant.now();
    protected User() {}
    public User(String provider, String providerSubject, String email, String displayName) { this.provider=provider; this.providerSubject=providerSubject; this.email=email; this.displayName=displayName; }
    public UUID getId(){return id;} public String getEmail(){return email;} public String getDisplayName(){return displayName;}
}
