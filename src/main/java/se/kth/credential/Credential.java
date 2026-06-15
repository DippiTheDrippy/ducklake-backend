package se.kth.credential;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "credentials")
public class Credential {

    @Id
    private UUID id;

    @Column(name = "dataset_id", nullable = false)
    private UUID datasetId;
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "postgres_username", nullable = false)
    private String postgresUsername;

    // Only used when creating secrets, not stored
    @Transient
    private String postgresPassword;

    @Column(name = "garage_access_key_id", nullable = false)
    private String garageAccessKeyId;

    // Only used when creating secrets, not stored
    @Transient
    private String garageSecretAccessKey;

    @Column(name = "expires_at", nullable = true)
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Credential(UUID datasetId, UUID userId, String postgresUsername, String garageAccessKeyId, OffsetDateTime expiresAt) {
        this.datasetId = datasetId;
        this.userId = userId;
        this.postgresUsername = postgresUsername;
        this.garageAccessKeyId = garageAccessKeyId;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();

        if (id == null) {
            id = UUID.randomUUID();
        }

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

}
