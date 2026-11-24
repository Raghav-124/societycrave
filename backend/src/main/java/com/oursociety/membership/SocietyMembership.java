package com.oursociety.membership;

import com.oursociety.society.Society;
import com.oursociety.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="societycrave_memberships", uniqueConstraints=@UniqueConstraint(name="uk_sc_membership_user_society", columnNames={"user_id","society_id"}))
public class SocietyMembership {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="user_id", nullable=false) private User user;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="society_id", nullable=false) private Society society;
    @Column(name="unit_number", nullable=false) private String unitNumber;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Status status;
    @Column(nullable=false) private Instant createdAt=Instant.now();
    protected SocietyMembership(){}
    public SocietyMembership(User u,Society s,String unit){user=u;society=s;unitNumber=unit;status=Status.ACTIVE;}
    public UUID getId(){return id;} public String getUnitNumber(){return unitNumber;} public Status getStatus(){return status;} public Society getSociety(){return society;}
    public enum Status { PENDING, ACTIVE, REJECTED }
}
