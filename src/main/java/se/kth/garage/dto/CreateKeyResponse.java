package se.kth.garage.dto;

import java.time.OffsetDateTime;

public record CreateKeyResponse(
        String accessKeyId,
        OffsetDateTime expiration,
        String name,
        String secretAccessKey
) {
}
