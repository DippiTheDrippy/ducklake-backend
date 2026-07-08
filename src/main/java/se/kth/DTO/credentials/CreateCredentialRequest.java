package se.kth.DTO.credentials;

import java.time.OffsetDateTime;

import se.kth.model.AccessLevel;

public record CreateCredentialRequest(
                String name,
                AccessLevel access,
                OffsetDateTime expiresAt,
                boolean neverExpires) {
}
