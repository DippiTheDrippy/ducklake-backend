package se.kth.DTO.dataset;

public record UpdateDatasetRequest(
                String displayName,
                String description,
                Boolean isPublic) {
}
