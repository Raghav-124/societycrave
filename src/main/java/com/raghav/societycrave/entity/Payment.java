package com.raghav.societycrave.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Resident name is mandatory")
    @Column(nullable = false)
    private String residentName;

    @NotBlank(message = "Flat number is mandatory")
    @Column(nullable = false)
    private String flatNumber;

    private String residentEmail;

    private String societyName;

    private Long orderId;

    @NotNull(message = "Amount is mandatory")
    @Column(nullable = false)
    private BigDecimal amount;

    private LocalDate dueDate;

    private LocalDate paymentDate;

    @Column(nullable = false)
    private String status = "DUE"; // DUE, PAID, OVERDUE

    private String paymentMethod;

    public Payment() {}

    public Payment(Long id, String residentName, String flatNumber, String residentEmail,
                   String societyName, Long orderId, BigDecimal amount, LocalDate dueDate,
                   LocalDate paymentDate, String status, String paymentMethod) {
        this.id = id;
        this.residentName = residentName;
        this.flatNumber = flatNumber;
        this.residentEmail = residentEmail;
        this.societyName = societyName;
        this.orderId = orderId;
        this.amount = amount;
        this.dueDate = dueDate;
        this.paymentDate = paymentDate;
        this.status = status != null ? status : "DUE"; // default
        this.paymentMethod = paymentMethod;
    }

    // ----------------------
    // Getters & Setters
    // ----------------------
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getResidentName() { return residentName; }
    public void setResidentName(String residentName) { this.residentName = residentName; }

    public String getFlatNumber() { return flatNumber; }
    public void setFlatNumber(String flatNumber) { this.flatNumber = flatNumber; }

    public String getResidentEmail() { return residentEmail; }
    public void setResidentEmail(String residentEmail) { this.residentEmail = residentEmail; }

    public String getSocietyName() { return societyName; }
    public void setSocietyName(String societyName) { this.societyName = societyName; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
}
