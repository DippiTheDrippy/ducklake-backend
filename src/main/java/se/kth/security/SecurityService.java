package se.kth.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import se.kth.common.Pagination;
import se.kth.security.dto.CreateGroupRequest;
import se.kth.security.dto.GroupWithAccess;
import se.kth.security.dto.UserWithAccess;
import se.kth.security.keycloak.Group;
import se.kth.security.keycloak.JwtUser;
import se.kth.security.keycloak.KeycloakRepository;
import se.kth.security.keycloak.User;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@ApplicationScoped
public class SecurityService {

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

    public Pagination<Group> listGroups(int pageIndex, int pageSize) {
        return keycloakRepository.getGroups(pageIndex, pageSize);
    }

    public Pagination<Group> listMyGroups(JwtUser kUser, int pageIndex, int pageSize) {
        return keycloakRepository.getGroupsForUser(kUser.id().toString(), pageIndex, pageSize);
    }

    public Pagination<Group> searchGroups(String search, int pageIndex, int pageSize) {
        return keycloakRepository.searchGroups(search, pageIndex, pageSize);
    }

    public Group getGroup(String id) {
        return keycloakRepository.getGroupById(id)
                .orElseThrow(() -> new NotFoundException("Group does not exist!"));
    }

    public List<User> getGroupMembers(String id) {
        return keycloakRepository.getGroupMembers(id);
    }

    public void updateGroupPermissions(String groupId, String datasetId, AccessLevel accessLevel) {
        permissionRepository.grantGroupAccess(
                UUID.fromString(datasetId),
                UUID.fromString(groupId),
                accessLevel);
    }

    public void deleteGroupPermissions(String groupId, String datasetId) {
        permissionRepository.deleteGroupAccess(
                UUID.fromString(datasetId),
                UUID.fromString(groupId));
    }

    public List<GroupWithAccess> getGroupsWithAccess(UUID datasetId) {
        Map<UUID, AccessLevel> accessByGroupId = permissionRepository.findGroupsWithAccess(datasetId);

        return accessByGroupId.entrySet()
                .stream()
                .flatMap(entry -> {
                    UUID groupId = entry.getKey();
                    AccessLevel accessLevel = entry.getValue();

                    return keycloakRepository.getGroupById(groupId.toString())
                            .stream()
                            .map(group -> new GroupWithAccess(group, accessLevel));
                })
                .toList();
    }

}
