package se.kth.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import se.kth.common.Pagination;
import se.kth.dataset.Dataset;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PermissionRepository {

  @Inject
  EntityManager entityManager;

  private static final UUID emptyUUID = new UUID(0L, 0L);

  @SuppressWarnings("unchecked")
  public Pagination<Dataset> findAccessibleDatasetsBySearch(
      UUID userId,
      List<UUID> groupIds,
      String search,
      int pageIndex,
      int pageSize) {
    if (search == null || search.isBlank()) {
      return findAccessibleDatasets(userId, groupIds, pageIndex, pageSize);
    }
    boolean hasGroups = groupIds != null && !groupIds.isEmpty();
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
                    WHERE dgp.dataset_id = d.id
                      AND dgp.group_id IN (:groupIds)
                      AND dgp.access_level IN ('READ', 'WRITE')
                )
            )
            """)
        .setParameter("userId", userId)
        .setParameter("pattern", pattern)
        .setParameter("groupIds", hasGroups ? groupIds : List.of(emptyUUID))
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
                    WHERE dgp.dataset_id = d.id
                      AND dgp.group_id IN (:groupIds)
                      AND dgp.access_level IN ('READ', 'WRITE')
                )
            )
            ORDER BY d.name
            """, Dataset.class)
        .setParameter("userId", userId)
        .setParameter("pattern", pattern)
        .setParameter("groupIds", hasGroups ? groupIds : List.of(emptyUUID))
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
  public Pagination<Dataset> findAccessibleDatasets(UUID userId, List<UUID> groupIds, int pageIndex, int pageSize) {
    boolean hasGroups = groupIds != null && !groupIds.isEmpty();
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
                WHERE dgp.dataset_id = d.id
                  AND dgp.group_id IN (:groupIds)
                  AND dgp.access_level IN ('READ', 'WRITE')
            )
            """)
        .setParameter("userId", userId)
        .setParameter("groupIds", hasGroups ? groupIds : List.of(emptyUUID))
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
                WHERE dgp.dataset_id = d.id
                  AND dgp.group_id IN (:groupIds)
                  AND dgp.access_level IN ('READ', 'WRITE')
            )

            ORDER BY d.name
            """, Dataset.class)
        .setParameter("userId", userId)
        .setParameter("groupIds", hasGroups ? groupIds : List.of(emptyUUID))
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
  public Optional<Dataset> findAccessibleDataset(UUID userId, List<UUID> groupIds, UUID datasetId) {
    boolean hasGroups = groupIds != null && !groupIds.isEmpty();
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
                      WHERE dgp.dataset_id = d.id
                        AND dgp.group_id IN (:groupIds)
                        AND dgp.access_level IN ('READ', 'WRITE')
                  )
              )
            LIMIT 1
            """, Dataset.class)
        .setParameter("userId", userId)
        .setParameter("datasetId", datasetId)
        .setParameter("groupIds", hasGroups ? groupIds : List.of(emptyUUID))
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
  public void deleteUserAccess(UUID datasetId, UUID userId) {
    entityManager.createNativeQuery("""
        DELETE FROM dataset_user_permissions
        WHERE dataset_id = :datasetId
          AND user_id = :userId
        """)
        .setParameter("datasetId", datasetId)
        .setParameter("userId", userId)
        .executeUpdate();
  }

  public Map<UUID, AccessLevel> findUserWithAccess(
      UUID datasetId) {
    @SuppressWarnings("unchecked")
    List<Object[]> rows = entityManager
        .createNativeQuery("""
            SELECT user_id, access_level
            FROM dataset_user_permissions
            WHERE dataset_id = :datasetId
            """)
        .setParameter("datasetId", datasetId)
        .getResultList();

    Map<UUID, AccessLevel> result = new HashMap<>();

    for (Object[] row : rows) {
      UUID userId = (UUID) row[0];
      AccessLevel accessLevel = AccessLevel.valueOf((String) row[1]);

      result.put(userId, accessLevel);
    }

    return result;
  }

  @Transactional
  public void grantGroupAccess(UUID datasetId, UUID groupId, AccessLevel accessLevel) {
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

  @Transactional
  public void deleteGroupAccess(UUID datasetId, UUID groupId) {
    entityManager.createNativeQuery("""
        DELETE FROM dataset_group_permissions
        WHERE dataset_id = :datasetId
          AND group_id = :groupId
        """)
        .setParameter("datasetId", datasetId)
        .setParameter("groupId", groupId)
        .executeUpdate();
  }

  public Map<UUID, AccessLevel> findGroupsWithAccess(
      UUID datasetId) {
    @SuppressWarnings("unchecked")
    List<Object[]> rows = entityManager
        .createNativeQuery("""
            SELECT group_id, access_level
            FROM dataset_group_permissions
            WHERE dataset_id = :datasetId
            """)
        .setParameter("datasetId", datasetId)
        .getResultList();

    Map<UUID, AccessLevel> result = new HashMap<>();

    for (Object[] row : rows) {
      UUID groupId = (UUID) row[0];
      AccessLevel accessLevel = AccessLevel.valueOf((String) row[1]);

      result.put(groupId, accessLevel);
    }

    return result;
  }

  public boolean hasAccessLevel(UUID userId, List<UUID> groupIds, UUID datasetId, AccessLevel requiredLevel) {
    boolean hasGroups = groupIds != null && !groupIds.isEmpty();
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
                          WHERE dgp.dataset_id = d.id
                            AND dgp.group_id IN (:groupIds)
                            AND dgp.access_level IN ('READ', 'WRITE')
                      )
                  )
            ) THEN 1 ELSE 0 END
            """)
        .setParameter("userId", userId)
        .setParameter("datasetId", datasetId)
        .setParameter("groupIds", hasGroups ? groupIds : List.of(emptyUUID))
        .setParameter("includePublic", includePublic)
        .setParameter("allowedLevels", List.of(allowedLevels))
        .getSingleResult();

    return result.intValue() == 1;
  }
}
