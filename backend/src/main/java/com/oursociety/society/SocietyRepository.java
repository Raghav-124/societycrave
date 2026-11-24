package com.oursociety.society;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;
public interface SocietyRepository extends JpaRepository<Society, UUID> {
    @Query("select s from Society s where (:name = '' or lower(s.name) like lower(concat('%',:name,'%'))) and (:locality = '' or lower(s.locality) like lower(concat('%',:locality,'%'))) and (:city = '' or lower(s.city) like lower(concat('%',:city,'%'))) and (:pinCode = '' or s.pinCode = :pinCode)")
    List<Society> search(@Param("name") String name, @Param("locality") String locality, @Param("city") String city, @Param("pinCode") String pinCode);
    List<Society> findAllByGeohashIn(List<String> geohashes);
}
