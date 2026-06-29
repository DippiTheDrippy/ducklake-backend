package se.kth.admin.dto;

public record UpdateDatasetRequest(
                String displayName,
                String description,
                Boolean isPublic) {
}
