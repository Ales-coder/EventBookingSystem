package com.selenium.db;

import java.sql.Connection;

public class DbTest {
    public static void main(String[] args) throws Exception {
        try (Connection c = DB.getConnection()) {
            System.out.println("Connected " + c.getMetaData().getDatabaseProductName());
        }
    }
}
