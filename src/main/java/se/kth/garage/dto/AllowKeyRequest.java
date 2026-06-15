package se.kth.garage.dto;

public record AllowKeyRequest(
        String accessKeyId,
        String bucketId,
        KeyPermissions permissions
) {
}
