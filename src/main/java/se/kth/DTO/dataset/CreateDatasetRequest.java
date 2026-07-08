package se.kth.DTO.dataset;

public record CreateDatasetRequest(
        String name,
        String displayName,
        String description,
        Boolean isPublic) {
}
