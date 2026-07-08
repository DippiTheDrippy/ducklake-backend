package se.kth.DTO.dataset;

import java.util.List;

import se.kth.model.Dataset;
import se.kth.model.ducklake.TableSummary;

public record DatasetWithSummary(
        Dataset dataset,
        List<TableSummary> summary) {
}
