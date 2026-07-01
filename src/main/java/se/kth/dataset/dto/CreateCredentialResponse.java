package se.kth.dataset.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import se.kth.security.AccessLevel;

public record CreateCredentialResponse(
                UUID id,

                AccessLevel accessLevel,

                UUID datasetId,
                UUID userId,

                String database,
                String bucket,

                String postgresUsername,
                String postgresPassword,

                String garageAccessKeyId,
                String garageSecretAccessKey,

                OffsetDateTime expiresAt) {

}
