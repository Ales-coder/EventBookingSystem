package com.selenium.payment;

import java.math.BigDecimal;

public interface PaymentService {

    record PaymentRequest(
            String provider,
            BigDecimal amount,
            String cardHolderName,
            String cardNumber,
            String expiryMMYY,
            String cvv
    ) {}

    record PaymentResult(
            boolean approved,
            String message,
            String transactionId
    ) {}

    PaymentResult pay(PaymentRequest request);
}