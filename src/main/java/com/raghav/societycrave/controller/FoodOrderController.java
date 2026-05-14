package com.raghav.societycrave.controller;

import com.raghav.societycrave.entity.FoodOrder;
import com.raghav.societycrave.service.FoodOrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class FoodOrderController {

    private final FoodOrderService foodOrderService;

    public FoodOrderController(FoodOrderService foodOrderService) {
        this.foodOrderService = foodOrderService;
    }

    @GetMapping
    public List<FoodOrder> getAllOrders() {
        return foodOrderService.getAllOrders();
    }

    @GetMapping("/{id}")
    public FoodOrder getOrderById(@PathVariable Long id) {
        return foodOrderService.getOrderById(id);
    }

    @GetMapping("/status")
    public List<FoodOrder> getOrdersByStatus(@RequestParam String status) {
        return foodOrderService.getOrdersByStatus(status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FoodOrder createOrder(@Valid @RequestBody FoodOrder order) {
        return foodOrderService.saveOrder(order);
    }

    @PutMapping("/{id}")
    public FoodOrder updateOrder(@PathVariable Long id, @Valid @RequestBody FoodOrder orderDetails) {
        return foodOrderService.updateOrder(id, orderDetails);
    }

    @PutMapping("/{id}/status")
    public FoodOrder updateOrderStatus(@PathVariable Long id,
                                       @RequestParam String status,
                                       @RequestParam(value = "acceptedBy", required = false) String acceptedBy) {
        return foodOrderService.updateOrderStatus(id, status, acceptedBy);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrder(@PathVariable Long id) {
        foodOrderService.deleteOrder(id);
    }
}
