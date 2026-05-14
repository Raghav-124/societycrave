package com.raghav.societycrave;

import com.raghav.societycrave.repository.ChefRepository;
import com.raghav.societycrave.repository.ComplaintRepository;
import com.raghav.societycrave.repository.FoodOrderRepository;
import com.raghav.societycrave.repository.FoodRepository;
import com.raghav.societycrave.repository.PaymentRepository;
import com.raghav.societycrave.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TestController {

    private final ChefRepository chefRepository;
    private final FoodRepository foodRepository;
    private final FoodOrderRepository foodOrderRepository;
    private final ComplaintRepository complaintRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    public TestController(ChefRepository chefRepository,
                          FoodRepository foodRepository,
                          FoodOrderRepository foodOrderRepository,
                          ComplaintRepository complaintRepository,
                          PaymentRepository paymentRepository,
                          UserRepository userRepository) {
        this.chefRepository = chefRepository;
        this.foodRepository = foodRepository;
        this.foodOrderRepository = foodOrderRepository;
        this.complaintRepository = complaintRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/api/health")
    public String home() {
        return "SocietyCrave Backend Running 🚀";
    }

    @PostMapping("/api/admin/clear-non-resident-data")
    public Map<String, Object> clearNonResidentData() {
        long chefs = chefRepository.count();
        long foods = foodRepository.count();
        long orders = foodOrderRepository.count();
        long complaints = complaintRepository.count();
        long payments = paymentRepository.count();

        foodOrderRepository.deleteAllInBatch();
        foodRepository.deleteAllInBatch();
        complaintRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
        chefRepository.deleteAllInBatch();

        return Map.of(
                "status", "ok",
                "message", "Cleared all non-resident data. Residents are preserved.",
                "deleted", Map.of(
                        "chefs", chefs,
                        "foods", foods,
                        "orders", orders,
                        "complaints", complaints,
                        "payments", payments
                )
        );
    }

    @PostMapping("/api/admin/clear-all-data")
    public Map<String, Object> clearAllData() {
        long chefs = chefRepository.count();
        long foods = foodRepository.count();
        long orders = foodOrderRepository.count();
        long complaints = complaintRepository.count();
        long payments = paymentRepository.count();
        long users = userRepository.count();

        foodOrderRepository.deleteAllInBatch();
        foodRepository.deleteAllInBatch();
        complaintRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
        chefRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        return Map.of(
                "status", "ok",
                "message", "Cleared all data including residents.",
                "deleted", Map.of(
                        "chefs", chefs,
                        "foods", foods,
                        "orders", orders,
                        "complaints", complaints,
                        "payments", payments,
                        "users", users
                )
        );
    }
}
