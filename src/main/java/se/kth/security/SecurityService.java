package se.kth.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import se.kth.common.Pagination;
import se.kth.security.dto.CreateGroupRequest;
import se.kth.security.group.Group;
import se.kth.security.group.GroupRepository;
import se.kth.security.user.User;
import se.kth.security.user.UserRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
@ApplicationScoped
public class SecurityService {

    @Inject
    UserRepository userRepository;
    @Inject
    GroupRepository groupRepository;
    @Inject
    PermissionRepository permissionRepository;

    public Pagination<User> listUsers(int pageIndex, int pageSize) {
        return userRepository.listAll(pageIndex, pageSize);
    }

    public User getUser(String id) {
        UUID realId = UUID.fromString(id);

        return userRepository.findById(realId);
    }

    public User getMyself(KeycloakUser user) {
        return userRepository.findByEmail(user.email())
                .orElseThrow(() -> new IllegalArgumentException("User does not exist!"));
    }

    public User register(KeycloakUser user) {
        return userRepository.upsertByEmail(user.email(), user.firstName(), user.lastName());
    }

    public void updateUserPermissions(String id, String datasetId, AccessLevel accessLevel) {
        permissionRepository.grantUserAccess(
                UUID.fromString(datasetId),
                UUID.fromString(id),
                accessLevel);
    }

    public void deleteUser(String id) {
        if (!userRepository.deleteByIdSafe(UUID.fromString(id)))
            log.warn("Failed to delete user");
    }

    public Pagination<Group> listGroups(int pageIndex, int pageSize) {
        return groupRepository.listAll(pageIndex, pageSize);
    }

    public Pagination<Group> listMyGroups(KeycloakUser kUser, int pageIndex, int pageSize) {
        User user = userRepository.findByEmail(kUser.email())
                .orElseThrow(() -> new IllegalArgumentException("User does not exist: " + kUser.email()));

        return groupRepository.findGroupsForUser(user.getId(), pageIndex, pageSize);
    }

    public Group getGroup(String id) {
        return groupRepository.findById(UUID.fromString(id));
    }

    public Group getGroupIfMember(String id, KeycloakUser kUser) {
        User user = userRepository.findByEmail(kUser.email())
                .orElseThrow(() -> new IllegalArgumentException("User does not exist: " + kUser.email()));

        return groupRepository.findGroupIfMember(user.getId(), id).orElse(null);
    }

    public Group createGroup(@Valid CreateGroupRequest req) {
        return groupRepository.upsertByName(req.name(), req.displayName(), req.description());
    }

    public void updateGroupPermissions(String id, String datasetId, AccessLevel accessLevel) {
        permissionRepository.grantGroupAccess(
                UUID.fromString(datasetId),
                UUID.fromString(id),
                accessLevel);
    }

    public void deleteGroup(String id) {
        if (!groupRepository.deleteByIdSafe(UUID.fromString(id)))
            log.warn("Failed to delete group");
    }

}
