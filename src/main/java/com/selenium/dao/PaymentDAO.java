package com.selenium.dao;

import com.selenium.db.DB;

import java.math.BigDecimal;
import java.sql.*;

public class PaymentDAO {

    public long createPayment(long bookingId,
                              String provider,
                              String providerOrderId,
                              BigDecimal amount) throws SQLException {

        String sql = """
            INSERT INTO payments (booking_id, provider, provider_order_id, amount, status)
            VALUES (?, ?, ?, ?, 'PENDING')
            RETURNING payment_id
        """;

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, bookingId);
            ps.setString(2, provider);
            ps.setString(3, providerOrderId);
            ps.setBigDecimal(4, amount);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public void markPaymentCompleted(long paymentId,
                                     String captureId) throws SQLException {

        String sql = """
            UPDATE payments
            SET status='COMPLETED',
                provider_capture_id=?
            WHERE payment_id=?
        """;

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, captureId);
            ps.setLong(2, paymentId);
            ps.executeUpdate();
        }
    }

    public void markPaymentFailed(long paymentId) throws SQLException {

        String sql = "UPDATE payments SET status='FAILED' WHERE payment_id=?";

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, paymentId);
            ps.executeUpdate();
        }
    }
}