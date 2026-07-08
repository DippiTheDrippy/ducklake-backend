package se.kth.DTO.garage;

import java.time.OffsetDateTime;

public record CreateKeyRequest(
                String name,
                OffsetDateTime expiration,
                Boolean neverExpires,
                KeyPerm allow,
                KeyPerm deny) {

        public CreateKeyRequest(String name, OffsetDateTime expiration, boolean neverExpires) {
                this(
                                name,
                                neverExpires ? null : expiration,
                                neverExpires,
                                null,
                                null);
        }
}