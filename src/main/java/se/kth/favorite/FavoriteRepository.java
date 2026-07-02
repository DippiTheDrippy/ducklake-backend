package se.kth.favorite;

import java.util.List;
import java.util.UUID;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import se.kth.common.Pagination;
import se.kth.dataset.Dataset;

@ApplicationScoped
public class FavoriteRepository implements PanacheRepositoryBase<Favorite, FavoriteId> {

    @SuppressWarnings("unchecked")
    public Pagination<Dataset> listFavortiedDatasets(UUID userId, int pageIndex, int pageSize) {
        Number totalItems = (Number) getEntityManager()
                .createQuery("""
                            SELECT COUNT(f.dataset)
                            FROM Favorite f
                            WHERE f.userId = :userId
                        """)
                .setParameter("userId", userId)
                .getSingleResult();
        long total = totalItems.longValue();

        List<Dataset> datasets = getEntityManager()
                .createQuery("""
                            SELECT f.dataset
                            FROM Favorite f
                            WHERE f.userId = :userId
                            ORDER BY f.createdAt DESC
                        """).setParameter("userId", userId)
                .setFirstResult(pageIndex * pageSize)
                .setMaxResults(pageSize)
                .getResultList();

        return new Pagination<>(datasets, pageIndex, pageSize, total);
    }

    public boolean isFavorited(UUID userId, UUID datasetId) {
        return find("userId = ?1 and datasetId = ?2", userId, datasetId)
                .firstResultOptional()
                .isPresent();
    }

    @Transactional
    public void addFavorite(UUID userId, UUID datasetId) {
        if (!isFavorited(userId, datasetId)) {
            persist(new Favorite(userId, datasetId));
        }
    }

    @Transactional
    public boolean removeFavorite(UUID userId, UUID datasetId) {
        return delete("userId = ?1 and datasetId = ?2", userId, datasetId) > 0;
    }

}
