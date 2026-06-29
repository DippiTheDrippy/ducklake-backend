package se.kth.security.group;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import se.kth.common.Pagination;
import se.kth.security.user.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class GroupRepository implements PanacheRepositoryBase<Group, UUID> {

    public Pagination<Group> findGroupsForUser(UUID userId, int pageIndex, int pageSize) {
        Number totalItems = (Number) getEntityManager()
                .createQuery("""
                        SELECT COUNT(DISTINCT g.id)
                        FROM Group g
                        JOIN g.users u
                        WHERE u.id = :userId
                        """)
                .setParameter("userId", userId)
                .getSingleResult();
        long total = totalItems.longValue();

        List<Group> groups = getEntityManager()
                .createQuery("""
                        SELECT DISTINCT g
                        FROM Group g
                        JOIN g.users u
                        WHERE u.id = :userId
                        ORDER BY g.name
                        """, Group.class)
                .setParameter("userId", userId)
                .setFirstResult(pageIndex * pageSize)
                .setMaxResults(pageSize)
                .getResultList();

        return new Pagination<>(groups, pageIndex, pageSize, total);
    }

    public Optional<Group> findGroupIfMember(UUID userId, String groupId) {
        return getEntityManager()
                .createQuery("""
                        SELECT DISTINCT g
                        FROM Group g
                        JOIN g.users u
                        WHERE u.id = :userId
                            AND g.id = :groupId
                        """, Group.class)
                .setParameter("userId", userId)
                .setParameter("groupId", groupId)
                .getResultStream().findFirst();
    }

    public Optional<Group> findByName(String name) {
        return find("name", name).firstResultOptional();
    }

    @Transactional
    public Group save(Group group) {
        persist(group);
        return group;
    }

    @Transactional
    public Group upsertByName(String name, String displayName, String description) {
        Optional<Group> existingGroup = findByName(name);

        if (existingGroup.isPresent()) {
            Group group = existingGroup.get();
            group.setName(name);
            group.setDisplayName(displayName);
            group.setDescription(description);
            return group;
        }

        Group group = new Group(name, displayName, description);
        persist(group);
        return group;
    }

    @Transactional
    public void addUserToGroup(UUID groupId, User user) {
        Group group = findByIdOptional(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found!"));

        group.getUsers().add(user);
    }

    @Transactional
    public void removeUserToGroup(UUID groupId, User user) {
        Group group = findByIdOptional(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found!"));

        group.getUsers().remove(user);
    }

    @Transactional
    public boolean deleteByIdSafe(UUID id) {
        return deleteById(id);
    }

    public Pagination<Group> listAll(int pageIndex, int pageSize) {
        PanacheQuery<Group> query = findAll();
        long total = query.count();

        return new Pagination<>(query.page(pageIndex, pageSize).list(), pageIndex, pageSize, total);
    }

}
