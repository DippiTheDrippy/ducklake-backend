package se.kth.util.sql;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface SqlConnectionWork<T> {
    T execute(Connection connection) throws SQLException;
}