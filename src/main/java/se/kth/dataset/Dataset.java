package se.kth.dataset;

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
@Table(name = "datasets")
public class Dataset {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;
    @Column(name = "display_name", nullable = false)
    private String displayName;
    @Column(nullable = false)
    private String description;

    @Column(name = "bucket_name", nullable = false, unique = true)
    private String bucketName;
    @Column(name = "metadata_schema", nullable = false, unique = true)
    private String metadataSchema;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Dataset(String name, String displayName, String description, String bucketName, String metadataSchema, boolean isPublic) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.bucketName = bucketName;
        this.metadataSchema = metadataSchema;
        this.isPublic = isPublic;
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
