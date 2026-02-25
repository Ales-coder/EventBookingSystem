package com.selenium.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DB {
    private static final String URL  = "jdbc:postgresql://localhost:5432/ticket_booking_db";
    //Default database credentials for testing
    private static final String USER = "postgres";
    private static final String PASS = "postgres";

    private DB() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}

