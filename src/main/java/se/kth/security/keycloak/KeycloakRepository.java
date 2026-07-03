package se.kth.security.keycloak;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import se.kth.common.Pagination;

@ApplicationScoped
public class KeycloakRepository {

    private static final boolean BRIEF_REPRESENTATION = true;

    @Inject
    @RestClient
    KeycloakAdminClient keycloakAdminClient;

    @ConfigProperty(name = "keycloak.admin.realm")
    String realm;

    public Pagination<User> getUsers(int pageIndex, int pageSize) {
        return searchUsers(null, pageIndex, pageSize);
    }

    public Pagination<User> searchUsers(String search, int pageIndex, int pageSize) {
        validatePagination(pageIndex, pageSize);

        int first = toFirstResult(pageIndex, pageSize);
        String normalizedSearch = normalizeSearch(search);

        List<User> users = keycloakAdminClient.getUsers(
                realm,
                first,
                pageSize,
                normalizedSearch,
                BRIEF_REPRESENTATION);

        long totalItems = keycloakAdminClient.countUsers(
                realm,
                normalizedSearch);

        return new Pagination<>(users, pageIndex, pageSize, totalItems);
    }

    public Optional<User> getUserById(String userId) {
        try {
            return Optional.ofNullable(keycloakAdminClient.getUserById(realm, userId));
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    public Pagination<Group> getGroups(int pageIndex, int pageSize) {
        return searchGroups(null, pageIndex, pageSize);
    }

    public Pagination<Group> searchGroups(String search, int pageIndex, int pageSize) {
        validatePagination(pageIndex, pageSize);

        int first = toFirstResult(pageIndex, pageSize);
        String normalizedSearch = normalizeSearch(search);

        List<Group> groups = keycloakAdminClient.getGroups(
                realm,
                first,
                pageSize,
                normalizedSearch,
                BRIEF_REPRESENTATION,
                false,
                false);

        long totalItems = extractCount(
                keycloakAdminClient.countGroups(realm, normalizedSearch));

        return new Pagination<>(groups, pageIndex, pageSize, totalItems);
    }

    public Optional<Group> getGroupById(String groupId) {
        try {
            return Optional.ofNullable(keycloakAdminClient.getGroupById(realm, groupId));
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    public Pagination<Group> getGroupsForUser(
            String userId,
            int pageIndex,
            int pageSize) {
        return searchGroupsForUser(userId, null, pageIndex, pageSize);
    }

    public Pagination<Group> searchGroupsForUser(
            String userId,
            String search,
            int pageIndex,
            int pageSize) {
        validatePagination(pageIndex, pageSize);

        int first = toFirstResult(pageIndex, pageSize);
        String normalizedSearch = normalizeSearch(search);

        List<Group> groups = keycloakAdminClient.getGroupsForUser(
                realm,
                userId,
                first,
                pageSize,
                normalizedSearch,
                BRIEF_REPRESENTATION);

        long totalItems = extractCount(
                keycloakAdminClient.countGroupsForUser(realm, userId, normalizedSearch));

        return new Pagination<>(groups, pageIndex, pageSize, totalItems);
    }

    public Pagination<User> getGroupMembers(
            String groupId,
            int pageIndex,
            int pageSize) {
        validatePagination(pageIndex, pageSize);

        int first = toFirstResult(pageIndex, pageSize);

        List<User> users = keycloakAdminClient.getGroupMembers(
                realm,
                groupId,
                first,
                pageSize,
                BRIEF_REPRESENTATION);

        /*
         * Keycloak's group members endpoint supports first/max pagination,
         * but there is no dedicated /groups/{groupId}/members/count endpoint.
         *
         * Therefore, we cannot get a reliable totalItems without extra work.
         * Returning the visible page size as totalItems keeps this method usable,
         * but totalPages will only represent the current fetched result.
         */
        return new Pagination<>(users, pageIndex, pageSize, users.size());
    }

    private int toFirstResult(int pageIndex, int pageSize) {
        return pageIndex * pageSize;
    }

    private void validatePagination(int pageIndex, int pageSize) {
        if (pageIndex < 0) {
            throw new IllegalArgumentException("pageIndex must be greater than or equal to 0");
        }

        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be greater than 0");
        }
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }

        return search.trim();
    }

    private long extractCount(Map<String, Long> response) {
        if (response == null || response.isEmpty()) {
            return 0L;
        }

        Long count = response.get("count");

        if (count != null) {
            return count;
        }

        return response.values()
                .stream()
                .findFirst()
                .orElse(0L);
    }
}