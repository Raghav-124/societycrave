package com.raghav.societycrave.service;

import com.raghav.societycrave.entity.Payment;
import com.raghav.societycrave.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found with id " + id));
    }

    public Payment savePayment(Payment payment) {
        return paymentRepository.save(payment);
    }

    public Payment updatePayment(Long id, Payment paymentDetails) {
        Payment payment = getPaymentById(id);
        payment.setResidentName(paymentDetails.getResidentName());
        payment.setFlatNumber(paymentDetails.getFlatNumber());
        payment.setAmount(paymentDetails.getAmount());
        payment.setDueDate(paymentDetails.getDueDate());
        payment.setPaymentDate(paymentDetails.getPaymentDate());
        payment.setStatus(paymentDetails.getStatus());
        payment.setPaymentMethod(paymentDetails.getPaymentMethod());
        return paymentRepository.save(payment);
    }

    public void deletePayment(Long id) {
        paymentRepository.deleteById(id);
    }

    public List<Payment> getPaymentsByStatus(String status) {
        return paymentRepository.findByStatusIgnoreCase(status);
    }
}