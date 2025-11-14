package com.acl.backend;

import java.sql.Connection;
import java.sql.DriverManager;

public class TestConection {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://aws-1-us-east-2.pooler.supabase.com:6643/postgres?sslmode=require&prepareThreshold=0";
        String user = "postgres.fsrlutdwhqkzbsbvnktt";
        String password = "ACPATRONES25";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("✅ Conexión exitosa!");
            System.out.println("Database: " + conn.getCatalog());
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
