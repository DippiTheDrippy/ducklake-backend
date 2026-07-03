package se.kth.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import se.kth.common.Pagination;
import se.kth.dataset.Dataset;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PermissionRepository {

  @Inject
  EntityManager entityManager;

  @SuppressWarnings("unchecked")
  public Pagination<Dataset> findAccessibleDatasetsBySearch(
      UUID userId,
      String search,
      int pageIndex,
      int pageSize) {
    if (search == null || search.isBlank()) {
      return findAccessibleDatasets(userId, pageIndex, pageSize);
    }

    String pattern = "%" + search.trim() + "%";

    Number totalItems = (Number) entityManager
        .createNativeQuery("""
            SELECT COUNT(DISTINCT d.id)
            FROM datasets d
            WHERE (
                d.name ILIKE :pattern
                OR COALESCE(d.description, '') ILIKE :pattern
                OR COALESCE(d.display_name, '') ILIKE :pattern
            )
            AND (
                d.is_public = true

                OR EXISTS (
                    SELECT 1
                    FROM dataset_user_permissions dup
                    WHERE dup.dataset_id = d.id
                      AND dup.user_id = :userId
                      AND dup.access_level IN ('READ', 'WRITE')
                )

                OR EXISTS (
                    SELECT 1
                    FROM dataset_group_permissions dgp
                    JOIN user_group_members ugm
                      ON ugm.group_id = dgp.group_id
                    WHERE dgp.dataset_id = d.id
                      AND ugm.user_id = :userId
                      AND dgp.access_level IN ('READ', 'WRITE')
                )
            )
            """)
        .setParameter("userId", userId)
        .setParameter("pattern", pattern)
        .getSingleResult();
    long total = totalItems.longValue();

    List<Dataset> datasets = entityManager
        .createNativeQuery("""
            SELECT DISTINCT d.*
            FROM datasets d
            WHERE (
                d.name ILIKE :pattern
                OR COALESCE(d.description, '') ILIKE :pattern
                OR COALESCE(d.display_name, '') ILIKE :pattern
            )
            AND (
                d.is_public = true

                OR EXISTS (
                    SELECT 1
                    FROM dataset_user_permissions dup
                    WHERE dup.dataset_id = d.id
                      AND dup.user_id = :userId
                      AND dup.access_level IN ('READ', 'WRITE')
                )

                OR EXISTS (
                    SELECT 1
                    FROM dataset_group_permissions dgp
                    JOIN user_group_members ugm
                      ON ugm.group_id = dgp.group_id
                    WHERE dgp.dataset_id = d.id
                      AND ugm.user_id = :userId
                      AND dgp.access_level IN ('READ', 'WRITE')
                )
            )
            ORDER BY d.name
            """, Dataset.class)
        .setParameter("userId", userId)
        .setParameter("pattern", pattern)
        .setFirstResult(pageIndex * pageSize)
        .setMaxResults(pageSize)
        .getResultList();

    return new Pagination<>(datasets, pageIndex, pageSize, total);
  }

  /**
   * Lists all datasets the user can access, either because the dataset is public
   * or because the user has direct/group-based READ or WRITE access.
   */
  @SuppressWarnings("unchecked")
  public Pagination<Dataset> findAccessibleDatasets(UUID userId, int pageIndex, int pageSize) {
    Number totalItems = (Number) entityManager
        .createNativeQuery("""
            SELECT COUNT(DISTINCT d.id)
            FROM datasets d
            WHERE d.is_public = true

            OR EXISTS (
                SELECT 1
                FROM dataset_user_permissions dup
                WHERE dup.dataset_id = d.id
                  AND dup.user_id = :userId
                  AND dup.access_level IN ('READ', 'WRITE')
            )

            OR EXISTS (
                SELECT 1
                FROM dataset_group_permissions dgp
                JOIN user_group_members ugm
                  ON ugm.group_id = dgp.group_id
                WHERE dgp.dataset_id = d.id
                  AND ugm.user_id = :userId
                  AND dgp.access_level IN ('READ', 'WRITE')
            )
            """)
        .setParameter("userId", userId)
        .getSingleResult();
    long total = totalItems.longValue();

    List<Dataset> datasets = entityManager
        .createNativeQuery("""
            SELECT DISTINCT d.*
            FROM datasets d
            WHERE d.is_public = true

            OR EXISTS (
                SELECT 1
                FROM dataset_user_permissions dup
                WHERE dup.dataset_id = d.id
                  AND dup.user_id = :userId
                  AND dup.access_level IN ('READ', 'WRITE')
            )

            OR EXISTS (
                SELECT 1
                FROM dataset_group_permissions dgp
                JOIN user_group_members ugm
                  ON ugm.group_id = dgp.group_id
                WHERE dgp.dataset_id = d.id
                  AND ugm.user_id = :userId
                  AND dgp.access_level IN ('READ', 'WRITE')
            )

            ORDER BY d.name
            """, Dataset.class)
        .setParameter("userId", userId)
        .setFirstResult(pageIndex * pageSize)
        .setMaxResults(pageSize)
        .getResultList();

    return new Pagination<>(datasets, pageIndex, pageSize, total);
  }

  /**
   * Finds a dataset if the user has access to it, either because it is public
   * or because the user has direct/group-based READ or WRITE access.
   */
  @SuppressWarnings("unchecked")
  public Optional<Dataset> findAccessibleDataset(UUID userId, UUID datasetId) {
    List<Dataset> result = entityManager
        .createNativeQuery("""
            SELECT DISTINCT d.*
            FROM datasets d
            WHERE d.id = :datasetId
              AND (
                  d.is_public = true

                  OR EXISTS (
                      SELECT 1
                      FROM dataset_user_permissions dup
                      WHERE dup.dataset_id = d.id
                        AND dup.user_id = :userId
                        AND dup.access_level IN ('READ', 'WRITE')
                  )

                  OR EXISTS (
                      SELECT 1
                      FROM dataset_group_permissions dgp
                      JOIN user_group_members ugm
                        ON ugm.group_id = dgp.group_id
                      WHERE dgp.dataset_id = d.id
                        AND ugm.user_id = :userId
                        AND dgp.access_level IN ('READ', 'WRITE')
                  )
              )
            LIMIT 1
            """, Dataset.class)
        .setParameter("userId", userId)
        .setParameter("datasetId", datasetId)
        .getResultList();

    return result.stream().findFirst();
  }

  /*
   * Methods below upserts the users access to the dataset
   */

  @Transactional
  public void grantUserAccess(UUID datasetId, UUID userId, AccessLevel accessLevel) {
    entityManager.createNativeQuery("""
        INSERT INTO dataset_user_permissions (dataset_id, user_id, access_level)
        VALUES (:datasetId, :userId, :accessLevel)
        ON CONFLICT (dataset_id, user_id)
        DO UPDATE SET access_level = EXCLUDED.access_level
        """)
        .setParameter("datasetId", datasetId)
        .setParameter("userId", userId)
        .setParameter("accessLevel", accessLevel.name())
        .executeUpdate();
  }

  @Transactional
  public void grantGroupAccess(UUID datasetId, String groupId, AccessLevel accessLevel) {
    entityManager.createNativeQuery("""
        INSERT INTO dataset_group_permissions (dataset_id, group_id, access_level)
        VALUES (:datasetId, :groupId, :accessLevel)
        ON CONFLICT (dataset_id, group_id)
        DO UPDATE SET access_level = EXCLUDED.access_level
        """)
        .setParameter("datasetId", datasetId)
        .setParameter("groupId", groupId)
        .setParameter("accessLevel", accessLevel.name())
        .executeUpdate();
  }

  public boolean hasAccessLevel(UUID userId, UUID datasetId, AccessLevel requiredLevel) {
    boolean includePublic = requiredLevel == AccessLevel.READ;

    String[] allowedLevels = switch (requiredLevel) {
      case READ -> new String[] { "READ", "WRITE" };
      case WRITE -> new String[] { "WRITE" };
    };

    Number result = (Number) entityManager
        .createNativeQuery("""
            SELECT CASE WHEN EXISTS (
                SELECT 1
                FROM datasets d
                WHERE d.id = :datasetId
                  AND (
                      (:includePublic = true AND d.is_public = true)

                      OR EXISTS (
                          SELECT 1
                          FROM dataset_user_permissions dup
                          WHERE dup.dataset_id = d.id
                            AND dup.user_id = :userId
                            AND dup.access_level IN (:allowedLevels)
                      )

                      OR EXISTS (
                          SELECT 1
                          FROM dataset_group_permissions dgp
                          JOIN user_group_members ugm
                            ON ugm.group_id = dgp.group_id
                          WHERE dgp.dataset_id = d.id
                            AND ugm.user_id = :userId
                            AND dgp.access_level IN (:allowedLevels)
                      )
                  )
            ) THEN 1 ELSE 0 END
            """)
        .setParameter("userId", userId)
        .setParameter("datasetId", datasetId)
        .setParameter("includePublic", includePublic)
        .setParameter("allowedLevels", List.of(allowedLevels))
        .getSingleResult();

    return result.intValue() == 1;
  }
}
