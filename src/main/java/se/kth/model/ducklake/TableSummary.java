package se.kth.model.ducklake;

/*
     * VARCHAR column_name,
     * VARCHAR column_type,
     * VARCHAR min,
     * VARCHAR max,
     * INT64 approx_unique,
     * VARCHAR avg,
     * VARCHAR std,
     * VARCHAR q25,
     * VARCHAR q50,
     * VARCHAR q75,
     * INT64 count,
     * DECIMAL null_percentage
     */
public record TableSummary(
        String columnName,
        String columnType,
        String min,
        String max,
        long approxUnique,
        String avg,
        String std,
        String q25,
        String q50,
        String q75,
        long rowCount,
        float null_percentage) {
}
