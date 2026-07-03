package se.kth.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import se.kth.common.Pagination;
import se.kth.security.dto.CreateGroupRequest;
import se.kth.security.keycloak.Group;
import se.kth.security.keycloak.JwtUser;
import se.kth.security.keycloak.KeycloakRepository;
import se.kth.security.keycloak.User;

import java.util.List;
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

    public User getUser(String id) {
        return keycloakRepository.getUserById(id)
                .orElseThrow(() -> new NotFoundException("User does not exist!"));
    }

    public User getMyself(JwtUser user) {
        return keycloakRepository.getUserById(user.id().toString())
                .orElseThrow(() -> new NotFoundException("User does not exist!"));
    }

    public void updateUserPermissions(String id, String datasetId, AccessLevel accessLevel) {
        permissionRepository.grantUserAccess(
                UUID.fromString(datasetId),
                UUID.fromString(id),
                accessLevel);
    }

    public Pagination<Group> listGroups(int pageIndex, int pageSize) {
        return keycloakRepository.getGroups(pageIndex, pageSize);
    }

    public Pagination<Group> listMyGroups(JwtUser kUser, int pageIndex, int pageSize) {
        return keycloakRepository.getGroupsForUser(kUser.id().toString(), pageIndex, pageSize);
    }

    public Group getGroup(String id) {
        return keycloakRepository.getGroupById(id)
                .orElseThrow(() -> new NotFoundException("Group does not exist!"));
    }

    public void updateGroupPermissions(String groupId, String datasetId, AccessLevel accessLevel) {
        permissionRepository.grantGroupAccess(
                UUID.fromString(datasetId),
                groupId,
                accessLevel);
    }

}
