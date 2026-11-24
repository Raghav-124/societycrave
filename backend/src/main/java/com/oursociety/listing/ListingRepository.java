package com.oursociety.listing;
import com.oursociety.chef.Chef;
import com.oursociety.society.Society;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.UUID;
public interface ListingRepository extends JpaRepository<Listing,UUID> {
 List<Listing> findAllBySocietyAndStatus(Society society, Listing.Status status);
 List<Listing> findAllByChef(Chef chef);
}
