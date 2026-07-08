package se.kth.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import se.kth.DTO.user.UserWithAccess;
import se.kth.common.Pagination;
import se.kth.model.AccessLevel;
import se.kth.model.User;
import se.kth.repositories.KeycloakRepository;
import se.kth.repositories.PermissionRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@ApplicationScoped
public class UserService {

    @Inject
    KeycloakRepository keycloakRepository;

    @Inject
    PermissionRepository permissionRepository;

    public Pagination<User> listUsers(int pageIndex, int pageSize) {
        return keycloakRepository.getUsers(pageIndex, pageSize);
    }

    public Pagination<User> searchUsers(String search, int pageIndex, int pageSize) {
        return keycloakRepository.searchUsers(search, pageIndex, pageSize);
    }

    public User getUser(String id) {
        return keycloakRepository.getUserById(id)
                .orElseThrow(() -> new NotFoundException("User does not exist!"));
    }

    public void updateUserPermissions(String userId, String datasetId, AccessLevel accessLevel) {
        permissionRepository.grantUserAccess(
                UUID.fromString(datasetId),
                UUID.fromString(userId),
                accessLevel);
    }

    public void deleteUserPermissions(String userId, String datasetId) {
        permissionRepository.deleteUserAccess(
                UUID.fromString(datasetId),
                UUID.fromString(userId));
    }

    public List<UserWithAccess> getUsersWithAccess(UUID datasetId) {
        Map<UUID, AccessLevel> accessByUserId = permissionRepository.findUserWithAccess(datasetId);

        return accessByUserId.entrySet()
                .stream()
                .flatMap(entry -> {
                    UUID userId = entry.getKey();
                    AccessLevel accessLevel = entry.getValue();

                    return keycloakRepository.getUserById(userId.toString())
                            .stream()
                            .map(user -> new UserWithAccess(user, accessLevel));
                })
                .toList();
    }

}
