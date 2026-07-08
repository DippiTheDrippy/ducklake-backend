package se.kth.DTO.garage;

public record AllowKeyRequest(
                String accessKeyId,
                String bucketId,
                KeyPermissions permissions) {
}
