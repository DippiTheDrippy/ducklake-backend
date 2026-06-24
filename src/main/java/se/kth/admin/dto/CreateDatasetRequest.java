package se.kth.admin.dto;

public record CreateDatasetRequest(
                String name,
                String display_name,
                String description,
                Boolean is_public) {
}
