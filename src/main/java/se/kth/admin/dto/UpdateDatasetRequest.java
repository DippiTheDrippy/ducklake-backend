package se.kth.admin.dto;

public record UpdateDatasetRequest(
        String display_name,
        String description,
        Boolean is_public) {

}
