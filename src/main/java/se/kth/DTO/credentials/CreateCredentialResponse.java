package se.kth.DTO.credentials;

import java.time.OffsetDateTime;
import java.util.UUID;

import se.kth.model.AccessLevel;

public record CreateCredentialResponse(
                UUID id,

                AccessLevel accessLevel,

                String name,

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
