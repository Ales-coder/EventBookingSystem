package com.selenium.dao;

import com.selenium.db.DB;
import com.selenium.model.BookingHistoryItem;
import com.selenium.model.User;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BookingDAO {

    private final SecurityLogDAO securityLogDAO = new SecurityLogDAO();
    private static final int SEAT_ABUSE_LIMIT = 3;



    public long bookSingleSeat(User user,
                               long eventId,
                               long seatId,
                               BigDecimal price) throws SQLException {

        if (user == null)
            throw new SQLException("User required");


        int expiredCount = securityLogDAO.countByUserAndAction(
                user.getUserId(),
                "BOOK_EXPIRED_SEAT_" + seatId,
                525600
        );

        if (expiredCount >= SEAT_ABUSE_LIMIT) {

            securityLogDAO.log(
                    "WARN",
                    "BOOK_BLOCKED",
                    user.getUserId(),
                    user.getEmail(),
                    "Seat permanently blocked seatId=" + seatId
            );

            throw new SQLException("Seat permanently blocked due to abuse.");
        }

        String holdSeatSql = """
            UPDATE event_seats
            SET state='HELD',
                held_by_user_id=?,
                hold_expires_at=?
            WHERE event_id=?
              AND seat_id=?
              AND state='AVAILABLE'
        """;

        String insertBookingSql =
                "INSERT INTO bookings (user_id, event_id, status) " +
                        "VALUES (?, ?, 'PENDING') RETURNING booking_id";

        String insertItemSql =
                "INSERT INTO booking_items (booking_id, seat_id, price, event_id) " +
                        "VALUES (?, ?, ?, ?)";

        try (Connection con = DB.getConnection()) {

            con.setAutoCommit(false);

            try {

                Timestamp expiresAt =
                        Timestamp.valueOf(LocalDateTime.now().plusMinutes(2));

                try (PreparedStatement ps = con.prepareStatement(holdSeatSql)) {
                    ps.setLong(1, user.getUserId());
                    ps.setTimestamp(2, expiresAt);
                    ps.setLong(3, eventId);
                    ps.setLong(4, seatId);

                    if (ps.executeUpdate() == 0) {

                        securityLogDAO.log(
                                "INFO",
                                "BOOK_FAIL",
                                user.getUserId(),
                                user.getEmail(),
                                "Seat not available seatId=" + seatId
                        );

                        con.rollback();
                        throw new SQLException("Seat not available.");
                    }
                }

                long bookingId;

                try (PreparedStatement ps =
                             con.prepareStatement(insertBookingSql)) {

                    ps.setLong(1, user.getUserId());
                    ps.setLong(2, eventId);

                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        bookingId = rs.getLong(1);
                    }
                }

                try (PreparedStatement ps =
                             con.prepareStatement(insertItemSql)) {

                    ps.setLong(1, bookingId);
                    ps.setLong(2, seatId);
                    ps.setBigDecimal(3, price);
                    ps.setLong(4, eventId);
                    ps.executeUpdate();
                }

                con.commit();

                securityLogDAO.log(
                        "INFO",
                        "BOOK_OK",
                        user.getUserId(),
                        user.getEmail(),
                        "Booked seatId=" + seatId
                );

                return bookingId;

            } catch (Exception ex) {
                con.rollback();
                throw new SQLException(ex.getMessage(), ex);
            } finally {
                con.setAutoCommit(true);
            }
        }
    }


    public boolean canStartPayment(long userId,
                                   long bookingId) throws SQLException {

        String sql = """
            SELECT COUNT(*)
            FROM bookings b
            JOIN booking_items bi ON bi.booking_id = b.booking_id
            JOIN event_seats es
              ON es.event_id = b.event_id
             AND es.seat_id  = bi.seat_id
            WHERE b.booking_id = ?
              AND b.user_id    = ?
              AND b.status     = 'PENDING'
              AND es.state     = 'HELD'
              AND es.held_by_user_id = ?
              AND es.hold_expires_at > CURRENT_TIMESTAMP
        """;

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, bookingId);
            ps.setLong(2, userId);
            ps.setLong(3, userId);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }


    public void payBooking(long userId,
                           long bookingId) throws SQLException {

        String seatUpdateSql = """
            UPDATE event_seats
            SET state='BOOKED',
                held_by_user_id=NULL,
                hold_expires_at=NULL
            WHERE (event_id, seat_id) IN (
                SELECT event_id, seat_id
                FROM booking_items
                WHERE booking_id=?
            )
            AND state='HELD'
            AND held_by_user_id=?
        """;

        String bookingUpdateSql =
                "UPDATE bookings " +
                        "SET status='PAID', paid_at=CURRENT_TIMESTAMP " +
                        "WHERE booking_id=? AND user_id=? AND status='PENDING'";

        try (Connection con = DB.getConnection()) {

            con.setAutoCommit(false);

            try {

                try (PreparedStatement ps =
                             con.prepareStatement(seatUpdateSql)) {

                    ps.setLong(1, bookingId);
                    ps.setLong(2, userId);

                    if (ps.executeUpdate() == 0) {
                        con.rollback();
                        throw new SQLException("Seat hold expired.");
                    }
                }

                try (PreparedStatement ps =
                             con.prepareStatement(bookingUpdateSql)) {

                    ps.setLong(1, bookingId);
                    ps.setLong(2, userId);

                    if (ps.executeUpdate() == 0) {
                        con.rollback();
                        throw new SQLException("Payment failed.");
                    }
                }

                con.commit();

            } catch (Exception ex) {
                con.rollback();
                throw new SQLException(ex.getMessage(), ex);
            } finally {
                con.setAutoCommit(true);
            }
        }
    }


    public void cancelBooking(long userId,
                              long bookingId) throws SQLException {

        String seatReleaseSql = """
            UPDATE event_seats
            SET state='AVAILABLE',
                held_by_user_id=NULL,
                hold_expires_at=NULL
            WHERE (event_id, seat_id) IN (
                SELECT event_id, seat_id
                FROM booking_items
                WHERE booking_id=?
            )
            AND state='HELD'
        """;

        String cancelSql =
                "UPDATE bookings SET status='CANCELLED' " +
                        "WHERE booking_id=? AND user_id=?";

        try (Connection con = DB.getConnection()) {

            con.setAutoCommit(false);

            try {

                try (PreparedStatement ps =
                             con.prepareStatement(seatReleaseSql)) {
                    ps.setLong(1, bookingId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps =
                             con.prepareStatement(cancelSql)) {

                    ps.setLong(1, bookingId);
                    ps.setLong(2, userId);

                    if (ps.executeUpdate() == 0)
                        throw new SQLException("Cancel failed.");
                }

                con.commit();

            } catch (Exception ex) {
                con.rollback();
                throw new SQLException(ex.getMessage(), ex);
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    public List<BookingHistoryItem> getBookingHistory(long userId)
            throws SQLException {

        String sql = """
            SELECT
                b.booking_id, b.created_at, b.status,
                e.event_id, e.title, e.start_time, e.status AS event_status,
                s.seat_id,
                (s.section||'-'||s.row_label||s.seat_no) AS seat_label,
                bi.price, b.paid_at
            FROM bookings b
            JOIN booking_items bi ON bi.booking_id=b.booking_id
            JOIN seats s ON s.seat_id=bi.seat_id
            JOIN events e ON e.event_id=b.event_id
            LEFT JOIN event_seats es
              ON es.event_id=b.event_id
             AND es.seat_id=bi.seat_id
            WHERE b.user_id=?
              AND (
                    b.status='PAID'
                 OR (
                        b.status='PENDING'
                    AND es.state='HELD'
                    AND es.held_by_user_id=?
                 )
              )
            ORDER BY b.created_at DESC
        """;

        List<BookingHistoryItem> out = new ArrayList<>();

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setLong(2, userId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    out.add(new BookingHistoryItem(
                            rs.getLong("booking_id"),
                            rs.getTimestamp("created_at").toLocalDateTime(),
                            rs.getString("status"),
                            rs.getLong("event_id"),
                            rs.getString("title"),
                            rs.getTimestamp("start_time").toLocalDateTime(),
                            rs.getString("event_status"),
                            "ACTIVE".equalsIgnoreCase(rs.getString("event_status")),
                            rs.getLong("seat_id"),
                            rs.getString("seat_label"),
                            rs.getBigDecimal("price"),
                            rs.getTimestamp("paid_at") == null ? null :
                                    rs.getTimestamp("paid_at").toLocalDateTime()
                    ));
                }
            }
        }

        return out;
    }
}