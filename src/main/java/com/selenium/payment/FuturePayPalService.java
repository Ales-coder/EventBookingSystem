package com.selenium.payment;

public class FuturePayPalService implements PaymentService {

    @Override
    public PaymentResult pay(PaymentRequest request) {
        return new PaymentResult(false,
                "PayPal integration not implemented yet (future work).",
                null
        );
    }
}