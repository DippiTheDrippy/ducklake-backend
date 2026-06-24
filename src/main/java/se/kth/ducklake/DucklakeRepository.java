package se.kth.ducklake;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import se.kth.ducklake.model.TableSummary;
import se.kth.ducklake.util.DucklakeConnectionFactory;
import se.kth.ducklake.util.DucklakeJdbcExecutor;
import se.kth.ducklake.util.DucklakeSql;
import se.kth.ducklake.util.DucklakeConnectionFactory.DucklakeConnectionRequest;

@Slf4j
@ApplicationScoped
public class DucklakeRepository {

    @Inject
    DucklakeConnectionFactory factory;

    @Inject
    DucklakeJdbcExecutor executor;

    public void createTable(
            ConnectionArgs args,
            String table,
            String filePath) throws SQLException {
        DucklakeConnectionRequest req = factory.defaultConnectionRequest(
                args.database(), args.bucket(), args.garageKeyId(), args.garageSecret());

        try {
            executor.executeInTransaction(req, DucklakeSql.createTableFromFile(table, filePath));
        } catch (SQLException e) {
            log.error("Failed to create DuckLake table '{}' from file '{}'", table, filePath, e);
            throw e;
        }
    }

    public void dropTable(
            ConnectionArgs args,
            String table) throws SQLException {
        DucklakeConnectionRequest req = factory.defaultConnectionRequest(
                args.database(), args.bucket(), args.garageKeyId(), args.garageSecret());

        try {
            executor.executeInTransaction(req, DucklakeSql.dropTable(table));
        } catch (SQLException e) {
            log.error("Failed to drop DuckLake table '{}'", table, e);
            throw e;
        }
    }

    public void insertFile(
            ConnectionArgs args,
            String table,
            String filePath) throws SQLException {
        DucklakeConnectionRequest req = factory.defaultConnectionRequest(
                args.database(), args.bucket(), args.garageKeyId(), args.garageSecret());

        try {
            executor.executeInTransaction(req, DucklakeSql.insertFileIntoTable(table, filePath));
        } catch (SQLException e) {
            log.error("Failed to insert '{}' into DuckLake table '{}'", filePath, table, e);
            throw e;
        }
    }

    public List<TableSummary> summary(
            ConnectionArgs args,
            String table) throws SQLException {
        DucklakeConnectionRequest req = factory.defaultConnectionRequest(
                args.database(), args.bucket(), args.garageKeyId(), args.garageSecret());

        try {
            return executor.query(req, DucklakeSql.summarize(table),
                    rs -> new TableSummary(
                            rs.getString("column_name"),
                            rs.getString("column_type"),
                            rs.getString("min"),
                            rs.getString("max"),
                            rs.getLong("approx_unique"),
                            rs.getString("avg"),
                            rs.getString("std"),
                            rs.getString("q25"),
                            rs.getString("q50"),
                            rs.getString("q75"),
                            rs.getLong("count"),
                            rs.getFloat("null_percentage")));
        } catch (SQLException e) {
            log.error("Failed to query summary from DuckLake table '{}'", table, e);
            throw e;
        }
    }

    public Optional<Long> totalRowCount(
            ConnectionArgs args,
            String table) throws SQLException {
        DucklakeConnectionRequest req = factory.defaultConnectionRequest(
                args.database(), args.bucket(), args.garageKeyId(), args.garageSecret());

        try {
            return executor.queryOne(req, DucklakeSql.totalRowCount(table),
                    ps -> {
                    },
                    rs -> rs.getLong("row_count"));
        } catch (SQLException e) {
            log.error("Failed to query total row count from DuckLake table '{}'", table, e);
            throw e;
        }
    }

    public Optional<Long> tableByteSize(
            ConnectionArgs args,
            String table) throws SQLException {
        DucklakeConnectionRequest req = factory.defaultConnectionRequest(
                args.database(), args.bucket(), args.garageKeyId(), args.garageSecret());

        try {
            return executor.queryOne(req, DucklakeSql.tableFileSize(table),
                    ps -> {
                    },
                    rs -> rs.getLong("file_size_bytes"));
        } catch (SQLException e) {
            log.error("Failed to query table byte size from DuckLake table '{}'", table, e);
            throw e;
        }
    }

    public record ConnectionArgs(
            String database,
            String bucket,
            String garageKeyId,
            String garageSecret) {
    }
}
