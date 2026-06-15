package se.kth.garage.dto;

import java.time.OffsetDateTime;

public record CreateKeyRequest(
        OffsetDateTime expiration,
        String name,
        boolean neverExpires
) {
}
