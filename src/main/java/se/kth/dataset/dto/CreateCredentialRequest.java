package se.kth.dataset.dto;

import java.time.OffsetDateTime;

import se.kth.security.AccessLevel;

public record CreateCredentialRequest(
                String name,
                AccessLevel access,
                OffsetDateTime expiresAt,
                boolean neverExpires) {
}
