package se.kth.model.ducklake;

public record JsonSource(
        String rowSourcePath,
        String inferredRowShape) {
}
