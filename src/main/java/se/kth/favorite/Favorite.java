package se.kth.favorite;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import se.kth.dataset.Dataset;
import se.kth.security.keycloak.User;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@IdClass(FavoriteId.class)
@Table(name = "favorites")
public class Favorite {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Column(name = "dataset_id")
    private UUID datasetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id", insertable = false, updatable = false)
    private Dataset dataset;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public Favorite(UUID userId, UUID datasetId) {
        this.userId = userId;
        this.datasetId = datasetId;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}