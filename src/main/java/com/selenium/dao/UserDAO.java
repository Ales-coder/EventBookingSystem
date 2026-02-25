package com.selenium.dao;

import com.selenium.db.DB;
import com.selenium.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

public class UserDAO {

    public Optional<User> findByEmail(String email) throws SQLException {
        String sql = "SELECT user_id, full_name, email, password_hash, role, created_at " +
                "FROM users WHERE email = ?";

        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();

                User u = new User(
                        rs.getLong("user_id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("role"),
                        rs.getTimestamp("created_at") != null
                                ? rs.getTimestamp("created_at").toLocalDateTime()
                                : null
                );
                return Optional.of(u);
            }
        }
    }

    public User register(String fullName, String email, String passwordHash, String role) throws SQLException {
        String sql = "INSERT INTO users(full_name, email, password_hash, role) " +
                "VALUES (?, ?, ?, ?) " +
                "RETURNING user_id, created_at";

        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, fullName.trim());
            ps.setString(2, email.trim().toLowerCase());
            ps.setString(3, passwordHash);
            ps.setString(4, role);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                long id = rs.getLong("user_id");
                Timestamp ts = rs.getTimestamp("created_at");
                LocalDateTime createdAt = ts != null ? ts.toLocalDateTime() : null;

                return new User(id, fullName.trim(), email.trim().toLowerCase(), passwordHash, role, createdAt);
            }
        }
    }
}
