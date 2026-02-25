package com.selenium.dao;

import com.selenium.db.DB;
import com.selenium.model.Event;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventDAO {

    private final SecurityLogDAO logDao = new SecurityLogDAO();


    public List<Event> getActiveEvents() throws SQLException {
        List<Event> events = new ArrayList<>();

        String sql = """
                SELECT 
                    e.event_id,
                    e.title,
                    e.category,
                    e.start_time,
                    COALESCE(SUM(CASE WHEN es.state = 'AVAILABLE' THEN 1 ELSE 0 END), 0) AS seats_left
                FROM events e
                LEFT JOIN event_seats es ON es.event_id = e.event_id
                WHERE e.status = 'ACTIVE'
                GROUP BY e.event_id, e.title, e.category, e.start_time
                ORDER BY e.start_time
                """;

        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                events.add(new Event(
                        rs.getLong("event_id"),
                        rs.getString("title"),
                        rs.getString("category"),
                        rs.getTimestamp("start_time").toLocalDateTime(),
                        rs.getInt("seats_left")
                ));
            }
        }

        return events;
    }


    public List<Event> getRecommendedEventsForUser(long userId, int limit) throws SQLException {
        if (limit <= 0) limit = 8;
        if (limit > 50) limit = 50;

        List<Event> events = new ArrayList<>();

        String sql = """
                WITH fav AS (
                    SELECT e.category, COUNT(*) AS cnt
                    FROM bookings b
                    JOIN events e ON e.event_id = b.event_id
                    WHERE b.user_id = ?
                    GROUP BY e.category
                    ORDER BY cnt DESC
                    LIMIT 3
                )
                SELECT
                    e.event_id,
                    e.title,
                    e.category,
                    e.start_time,
                    COALESCE(SUM(CASE WHEN es.state = 'AVAILABLE' THEN 1 ELSE 0 END), 0) AS seats_left
                FROM events e
                LEFT JOIN event_seats es ON es.event_id = e.event_id
                WHERE e.status = 'ACTIVE'
                  AND e.start_time >= now()
                  -- If fav is empty -> recommend upcoming events (fallback)
                  AND (
                        NOT EXISTS (SELECT 1 FROM fav)
                        OR e.category IN (SELECT category FROM fav)
                  )
                  -- Don't recommend events user already booked
                  AND NOT EXISTS (
                        SELECT 1
                        FROM bookings b
                        WHERE b.user_id = ?
                          AND b.event_id = e.event_id
                  )
                GROUP BY e.event_id, e.title, e.category, e.start_time
                ORDER BY
                  CASE
                    WHEN EXISTS (SELECT 1 FROM fav) AND e.category IN (SELECT category FROM fav) THEN 0
                    ELSE 1
                  END,
                  e.start_time
                LIMIT ?
                """;

        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setLong(2, userId);
            ps.setInt(3, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    events.add(new Event(
                            rs.getLong("event_id"),
                            rs.getString("title"),
                            rs.getString("category"),
                            rs.getTimestamp("start_time").toLocalDateTime(),
                            rs.getInt("seats_left")
                    ));
                }
            }
        }

        return events;
    }


    public List<Event> getAllEventsAdmin() throws SQLException {
        List<Event> events = new ArrayList<>();

        String sql = """
                SELECT 
                    e.event_id,
                    e.title,
                    e.category,
                    e.start_time,
                    COALESCE(SUM(CASE WHEN es.state = 'AVAILABLE' THEN 1 ELSE 0 END), 0) AS seats_left
                FROM events e
                LEFT JOIN event_seats es ON es.event_id = e.event_id
                GROUP BY e.event_id, e.title, e.category, e.start_time
                ORDER BY e.start_time
                """;

        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                events.add(new Event(
                        rs.getLong("event_id"),
                        rs.getString("title"),
                        rs.getString("category"),
                        rs.getTimestamp("start_time").toLocalDateTime(),
                        rs.getInt("seats_left")
                ));
            }
        }

        return events;
    }


    public long createEvent(long venueId, String title, String description, String category, Timestamp startTime) throws SQLException {

        String sql = """
                INSERT INTO events (venue_id, title, description, category, start_time, status)
                VALUES (?, ?, ?, ?, ?, 'ACTIVE')
                RETURNING event_id
                """;

        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, venueId);
            ps.setString(2, title);
            ps.setString(3, description);
            ps.setString(4, category);
            ps.setTimestamp(5, startTime);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }


    public void softDeleteEvent(long adminUserId, long eventId) throws SQLException {

        String updEvent = """
                UPDATE events
                SET status='DELETED'
                WHERE event_id=? AND status <> 'DELETED'
                """;

        String blockSeats = """
                UPDATE event_seats
                SET state='BLOCKED'
                WHERE event_id=?
                  AND state <> 'BLOCKED'
                """;

        try (Connection con = DB.getConnection()) {
            con.setAutoCommit(false);

            try {
                int updated;
                try (PreparedStatement ps = con.prepareStatement(updEvent)) {
                    ps.setLong(1, eventId);
                    updated = ps.executeUpdate();
                }

                try (PreparedStatement ps = con.prepareStatement(blockSeats)) {
                    ps.setLong(1, eventId);
                    ps.executeUpdate();
                }

                con.commit();

                if (updated == 0) {
                    logDao.log("WARN", "ADMIN_DELETE_EVENT", adminUserId, null,
                            "eventId=" + eventId + " note=ALREADY_DELETED_OR_NOT_FOUND");
                } else {
                    logDao.log("INFO", "ADMIN_DELETE_EVENT", adminUserId, null,
                            "eventId=" + eventId + " result=SOFT_DELETED");
                }

            } catch (Exception ex) {
                con.rollback();
                logDao.log("ERROR", "ADMIN_DELETE_EVENT_ERROR", adminUserId, null,
                        "eventId=" + eventId + " ex=" + ex.getClass().getSimpleName() + " msg=" + ex.getMessage());
                if (ex instanceof SQLException se) throw se;
                throw new SQLException(ex.getMessage(), ex);
            } finally {
                con.setAutoCommit(true);
            }
        }
    }
}
