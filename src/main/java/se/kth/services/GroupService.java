package se.kth.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import se.kth.DTO.group.GroupWithAccess;
import se.kth.common.Pagination;
import se.kth.model.AccessLevel;
import se.kth.model.Group;
import se.kth.model.JwtUser;
import se.kth.model.User;
import se.kth.repositories.KeycloakRepository;
import se.kth.repositories.PermissionRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@ApplicationScoped
public class GroupService {

    @Inject
    KeycloakRepository keycloakRepository;

    @Inject
    PermissionRepository permissionRepository;

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
