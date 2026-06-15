package se.kth.security.dto;

public record CreateGroupRequest(
        String name,
        String displayName,
        String description
) {
}
