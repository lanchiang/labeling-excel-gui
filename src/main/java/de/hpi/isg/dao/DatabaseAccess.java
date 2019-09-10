package de.hpi.isg.dao;

import lombok.Getter;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manage the access to a database by maintaining batch writers and ensuring all data is written before performing a
 * read.
 *
 * @author Lan Jiang
 * @since 9/9/19
 */
public class DatabaseAccess {

    /**
     * The connection to the database.
     */
    @Getter
    private Connection connection;

    public DatabaseAccess() {
        // Todo: change the configuration
        try {
            Class.forName("org.postgresql.Driver");
            this.connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/line_function_db",
                    "Fuga", null);
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException {
        Process p = Runtime.getRuntime().exec("create.sh");
//        System.out.println(p.exitValue());
    }
}
