package com.selenium.payment;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class MockPaymentService implements PaymentService {

    @Override
    public PaymentResult pay(PaymentRequest request) {

        String tx = "MOCK-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        String when = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        return new PaymentResult(true, "Payment approved (mock) at " + when, tx);
    }
}