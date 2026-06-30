package se.kth.ducklake.model;

public record JsonSource(
        String rowSourcePath,
        String inferredRowShape) {
}
