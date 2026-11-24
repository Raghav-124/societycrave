package com.oursociety.membership;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;
public interface MembershipRepository extends JpaRepository<SocietyMembership,UUID> {
    boolean existsByUserIdAndSocietyId(UUID userId, UUID societyId);
    List<SocietyMembership> findAllByUserId(UUID userId);
}
