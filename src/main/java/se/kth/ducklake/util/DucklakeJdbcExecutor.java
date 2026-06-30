package se.kth.ducklake.util;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import se.kth.ducklake.util.DucklakeConnectionFactory.DucklakeConnectionRequest;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class DucklakeJdbcExecutor {

    private final DucklakeConnectionFactory connectionFactory;

    @Inject
    public DucklakeJdbcExecutor(DucklakeConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public int execute(
            DucklakeConnectionRequest request,
            String sql) throws SQLException {
        return execute(request, sql, ps -> {
        });
    }

    public int execute(
            DucklakeConnectionRequest request,
            String sql,
            SqlBinder binder) throws SQLException {
        try (Connection conn = connectionFactory.openConnection(request);
                PreparedStatement ps = conn.prepareStatement(sql)) {

            binder.bind(ps);
            return ps.executeUpdate();
        }
    }

    public <T> List<T> query(
            DucklakeConnectionRequest request,
            String sql,
            RowMapper<T> mapper) throws SQLException {
        return query(request, sql, ps -> {
        }, mapper);
    }

    public <T> List<T> query(
            DucklakeConnectionRequest request,
            String sql,
            SqlBinder binder,
            RowMapper<T> mapper) throws SQLException {
        try (Connection conn = connectionFactory.openConnection(request);
                PreparedStatement ps = conn.prepareStatement(sql)) {

            binder.bind(ps);

            try (ResultSet rs = ps.executeQuery()) {
                List<T> rows = new ArrayList<>();

                while (rs.next()) {
                    rows.add(mapper.map(rs));
                }

                return rows;
            }
        }
    }

    public <T> Optional<T> queryOne(
            DucklakeConnectionRequest request,
            String sql,
            RowMapper<T> mapper) throws SQLException {
        return queryOne(request, sql, ps -> {
        }, mapper);
    }

    public <T> Optional<T> queryOne(
            DucklakeConnectionRequest request,
            String sql,
            SqlBinder binder,
            RowMapper<T> mapper) throws SQLException {
        List<T> rows = query(request, sql, binder, mapper);

        if (rows.isEmpty()) {
            return Optional.empty();
        }

        if (rows.size() > 1) {
            throw new SQLException("Expected one row, got " + rows.size());
        }

        return Optional.of(rows.getFirst());
    }

    public void executeInTransaction(
            DucklakeConnectionRequest request,
            String sql) throws SQLException {

        inTransaction(request, conn -> {
            try (Statement st = conn.createStatement()) {
                st.execute(sql);
            }

            return null;
        });
    }

    public <T> T inTransaction(
            DucklakeConnectionRequest request,
            SqlWork<T> work) throws SQLException {
        try (Connection conn = connectionFactory.openConnection(request)) {
            boolean previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try {
                T result = work.run(conn);
                conn.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(previousAutoCommit);
            }
        }
    }

    @FunctionalInterface
    public interface SqlBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    @FunctionalInterface
    public interface SqlWork<T> {
        T run(Connection conn) throws SQLException;
    }
}