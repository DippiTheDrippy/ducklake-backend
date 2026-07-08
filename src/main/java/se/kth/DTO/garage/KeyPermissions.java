package se.kth.DTO.garage;

public record KeyPermissions(
        boolean owner,
        boolean read,
        boolean write
) {
}
