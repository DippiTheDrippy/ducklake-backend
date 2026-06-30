package se.kth.dataset.dto;

import java.util.List;

import se.kth.dataset.Dataset;
import se.kth.ducklake.model.TableSummary;

public record DatasetWithSummary(
        Dataset dataset,
        List<TableSummary> summary) {
}
