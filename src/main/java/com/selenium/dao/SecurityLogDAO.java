package com.selenium.dao;

import com.selenium.db.DB;
import com.selenium.model.SecurityLog;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SecurityLogDAO {


    public record EmailCount(String email, int count, LocalDateTime lastAt) {}


    public void log(String level, String action, Long userId, String email, String details) {
        String sql = """
                INSERT INTO security_logs(level, action, user_id, email, details)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, (level == null || level.isBlank()) ? "INFO" : level.trim().toUpperCase());
            ps.setString(2, (action == null) ? "UNKNOWN" : action.trim().toUpperCase());

            if (userId == null) ps.setNull(3, Types.BIGINT);
            else ps.setLong(3, userId);

            ps.setString(4, email);
            ps.setString(5, details);

            ps.executeUpdate();
        } catch (SQLException ignored) {

        }
    }


    public List<SecurityLog> getLatest(int limit) throws SQLException {
        if (limit <= 0) limit = 200;

        String sql = """
                SELECT log_id, created_at, level, action, user_id, email, details
                FROM security_logs
                ORDER BY created_at DESC, log_id DESC
                LIMIT ?
                """;

        List<SecurityLog> out = new ArrayList<>();

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }


    public List<SecurityLog> search(String level, String action, String emailLike, int limit) throws SQLException {
        if (limit <= 0) limit = 200;

        String sql = """
                SELECT log_id, created_at, level, action, user_id, email, details
                FROM security_logs
                WHERE (? IS NULL OR level = ?)
                  AND (? IS NULL OR action = ?)
                  AND (? IS NULL OR LOWER(COALESCE(email,'')) LIKE LOWER(?))
                ORDER BY created_at DESC, log_id DESC
                LIMIT ?
                """;

        String lvl = (level == null || level.isBlank()) ? null : level.trim().toUpperCase();
        String act = (action == null || action.isBlank()) ? null : action.trim().toUpperCase();
        String em  = (emailLike == null || emailLike.isBlank()) ? null : "%" + emailLike.trim().toLowerCase() + "%";

        List<SecurityLog> out = new ArrayList<>();

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, lvl);
            ps.setString(2, lvl);

            ps.setString(3, act);
            ps.setString(4, act);

            ps.setString(5, em);
            ps.setString(6, em);

            ps.setInt(7, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }

        return out;
    }


    public int countAction(String action, int minutesBack) {
        String sql = """
                SELECT COUNT(*)
                FROM security_logs
                WHERE action = ?
                  AND created_at >= now() - (? * INTERVAL '1 minute')
                """;

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, action.trim().toUpperCase());
            ps.setInt(2, minutesBack);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            return 0;
        }
    }


    public int countByUserAndAction(Long userId, String action, int minutesBack) {
        if (userId == null) return 0;

        String sql = """
                SELECT COUNT(*)
                FROM security_logs
                WHERE user_id = ?
                  AND action = ?
                  AND created_at >= now() - (? * INTERVAL '1 minute')
                """;

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setString(2, action.trim().toUpperCase());
            ps.setInt(3, minutesBack);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            return 0;
        }
    }


    public int countByEmailAndAction(String email, String action, int minutesBack) {
        if (email == null || email.isBlank()) return 0;

        String sql = """
                SELECT COUNT(*)
                FROM security_logs
                WHERE LOWER(COALESCE(email,'')) = LOWER(?)
                  AND action = ?
                  AND created_at >= now() - (? * INTERVAL '1 minute')
                """;

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email.trim());
            ps.setString(2, action.trim().toUpperCase());
            ps.setInt(3, minutesBack);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            return 0;
        }
    }


    public List<EmailCount> topEmailsByAction(String action, int minutesBack, int limit) throws SQLException {
        if (limit <= 0) limit = 10;

        String sql = """
                SELECT 
                    LOWER(COALESCE(email,'')) AS email,
                    COUNT(*) AS cnt,
                    MAX(created_at) AS last_at
                FROM security_logs
                WHERE action = ?
                  AND created_at >= now() - (? * INTERVAL '1 minute')
                  AND COALESCE(email,'') <> ''
                GROUP BY LOWER(COALESCE(email,''))
                ORDER BY cnt DESC, last_at DESC
                LIMIT ?
                """;

        List<EmailCount> out = new ArrayList<>();

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, action.trim().toUpperCase());
            ps.setInt(2, minutesBack);
            ps.setInt(3, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("last_at");
                    LocalDateTime lastAt = (ts == null) ? null : ts.toLocalDateTime();
                    out.add(new EmailCount(
                            rs.getString("email"),
                            rs.getInt("cnt"),
                            lastAt
                    ));
                }
            }
        }

        return out;
    }

    private SecurityLog map(ResultSet rs) throws SQLException {
        long logId = rs.getLong("log_id");
        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime createdAt = ts == null ? null : ts.toLocalDateTime();

        String level = rs.getString("level");
        String action = rs.getString("action");

        Object uo = rs.getObject("user_id");
        Long userId = (uo == null) ? null : ((Number) uo).longValue();

        String email = rs.getString("email");
        String details = rs.getString("details");

        return new SecurityLog(logId, createdAt, level, action, userId, email, details);
    }
}
