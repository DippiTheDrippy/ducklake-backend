package se.kth.postgres.util;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface SqlConnectionWork<T> {
    T execute(Connection connection) throws SQLException;
}