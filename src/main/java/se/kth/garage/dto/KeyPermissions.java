package se.kth.garage.dto;

public record KeyPermissions(
        boolean owner,
        boolean read,
        boolean write
) {
}
