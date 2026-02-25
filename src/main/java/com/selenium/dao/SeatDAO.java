package com.selenium.dao;

import com.selenium.db.DB;
import com.selenium.model.SeatInfo;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SeatDAO {

    private final SecurityLogDAO securityLogDAO = new SecurityLogDAO();


    public List<SeatInfo> getSeatsForEvent(long eventId) throws SQLException {

        String selectExpiredSql = """
            SELECT seat_id, held_by_user_id
            FROM event_seats
            WHERE state='HELD'
              AND hold_expires_at < CURRENT_TIMESTAMP
        """;

        String releaseExpiredSeatsSql = """
            UPDATE event_seats
            SET state='AVAILABLE',
                held_by_user_id=NULL,
                hold_expires_at=NULL
            WHERE state='HELD'
              AND hold_expires_at < CURRENT_TIMESTAMP
        """;

        String loadSeatsSql = """
            SELECT 
                s.seat_id,
                (s.section || '-' || s.row_label || s.seat_no) AS seat_label,
                es.price,
                es.state
            FROM event_seats es
            JOIN seats s ON s.seat_id = es.seat_id
            WHERE es.event_id = ?
            ORDER BY s.section, s.row_label, s.seat_no
        """;

        List<SeatInfo> out = new ArrayList<>();

        try (Connection con = DB.getConnection()) {


            try (PreparedStatement ps = con.prepareStatement(selectExpiredSql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    long seatId = rs.getLong("seat_id");
                    Object uo = rs.getObject("held_by_user_id");

                    if (uo != null) {

                        Long userId = ((Number) uo).longValue();

                        securityLogDAO.log(
                                "INFO",
                                "BOOK_EXPIRED_SEAT_" + seatId,
                                userId,
                                null,
                                "Seat expired seatId=" + seatId
                        );
                    }
                }
            }


            try (PreparedStatement ps = con.prepareStatement(releaseExpiredSeatsSql)) {
                ps.executeUpdate();
            }


            try (PreparedStatement ps = con.prepareStatement(loadSeatsSql)) {

                ps.setLong(1, eventId);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        out.add(new SeatInfo(
                                rs.getLong("seat_id"),
                                rs.getString("seat_label"),
                                rs.getBigDecimal("price"),
                                rs.getString("state")
                        ));
                    }
                }
            }
        }

        return out;
    }


    public int generateSeatsForVenue(long venueId,
                                     String section,
                                     List<String> rows,
                                     int seatsPerRow) throws SQLException {

        String sql = """
            INSERT INTO seats (venue_id, section, row_label, seat_no)
            VALUES (?, ?, ?, ?)
            ON CONFLICT ON CONSTRAINT uq_seat DO NOTHING
        """;

        int inserted = 0;

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            for (String row : rows) {
                for (int i = 1; i <= seatsPerRow; i++) {
                    ps.setLong(1, venueId);
                    ps.setString(2, section);
                    ps.setString(3, row);
                    ps.setInt(4, i);
                    ps.addBatch();
                }
            }

            int[] res = ps.executeBatch();
            for (int x : res) {
                if (x > 0 || x == Statement.SUCCESS_NO_INFO)
                    inserted++;
            }
        }

        return inserted;
    }


    public int attachVenueSeatsToEvent(long eventId,
                                       long venueId,
                                       BigDecimal price) throws SQLException {

        String sql = """
            INSERT INTO event_seats (event_id, seat_id, price, state)
            SELECT ?, s.seat_id, ?, 'AVAILABLE'
            FROM seats s
            WHERE s.venue_id = ?
            ON CONFLICT (event_id, seat_id) DO NOTHING
        """;

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, eventId);
            ps.setBigDecimal(2, price);
            ps.setLong(3, venueId);

            return ps.executeUpdate();
        }
    }


    public int countSeatsInVenue(long venueId) throws SQLException {

        String sql = "SELECT COUNT(*) FROM seats WHERE venue_id=?";

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, venueId);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }


    public long getVenueIdForEvent(long eventId) throws SQLException {

        String sql = "SELECT venue_id FROM events WHERE event_id=?";

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, eventId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next())
                    throw new SQLException("Event not found");

                return rs.getLong("venue_id");
            }
        }
    }
}