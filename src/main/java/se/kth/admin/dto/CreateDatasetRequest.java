package se.kth.admin.dto;

public record CreateDatasetRequest(
        String name,
        String displayName,
        String description,
        Boolean isPublic) {
}
