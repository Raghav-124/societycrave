package com.raghav.societycrave.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_order")
public class FoodOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Customer name is mandatory")
    @Column(nullable = false)
    private String customerName;

    @NotBlank(message = "Flat number is mandatory")
    @Column(nullable = false)
    private String flatNumber;

    @NotBlank(message = "Order items are mandatory")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String items;

    @NotNull(message = "Total amount is mandatory")
    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private String status = "PLACED"; // PLACED, ACCEPTED, DELIVERED

    private String acceptedBy;

    @Column(nullable = false)
    private LocalDateTime orderTime = LocalDateTime.now();

    private LocalDateTime deliveryTime;

    private BigDecimal discount = BigDecimal.ZERO;

    private BigDecimal deliveryCharge = BigDecimal.ZERO;

    private String paymentMethod;

    @NotBlank(message = "Society name is mandatory")
    @Column(nullable = false)
    private String societyName; // ensures delivery is within same society

    public FoodOrder() {}

    public FoodOrder(Long id, String customerName, String flatNumber, String items, BigDecimal totalAmount,
                     String status, String acceptedBy, LocalDateTime orderTime, LocalDateTime deliveryTime,
                     BigDecimal discount, BigDecimal deliveryCharge, String paymentMethod, String societyName) {
        this.id = id;
        this.customerName = customerName;
        this.flatNumber = flatNumber;
        this.items = items;
        this.totalAmount = totalAmount;
        this.status = status;
        this.acceptedBy = acceptedBy;
        this.orderTime = orderTime;
        this.deliveryTime = deliveryTime;
        this.discount = discount;
        this.deliveryCharge = deliveryCharge;
        this.paymentMethod = paymentMethod;
        this.societyName = societyName;
    }

    // ----------------------
    // Getters & Setters
    // ----------------------
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getFlatNumber() { return flatNumber; }
    public void setFlatNumber(String flatNumber) { this.flatNumber = flatNumber; }

    public String getItems() { return items; }
    public void setItems(String items) { this.items = items; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAcceptedBy() { return acceptedBy; }
    public void setAcceptedBy(String acceptedBy) { this.acceptedBy = acceptedBy; }

    public LocalDateTime getOrderTime() { return orderTime; }
    public void setOrderTime(LocalDateTime orderTime) { this.orderTime = orderTime; }

    public LocalDateTime getDeliveryTime() { return deliveryTime; }
    public void setDeliveryTime(LocalDateTime deliveryTime) { this.deliveryTime = deliveryTime; }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }

    public BigDecimal getDeliveryCharge() { return deliveryCharge; }
    public void setDeliveryCharge(BigDecimal deliveryCharge) { this.deliveryCharge = deliveryCharge; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getSocietyName() { return societyName; }
    public void setSocietyName(String societyName) { this.societyName = societyName; }
}
