package com.oursociety.order;
import com.oursociety.chef.Chef; import com.oursociety.user.User; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List; import java.util.UUID;
public interface FoodOrderRepository extends JpaRepository<FoodOrder,UUID> { List<FoodOrder> findAllByResident(User resident); List<FoodOrder> findAllByChef(Chef chef); }
