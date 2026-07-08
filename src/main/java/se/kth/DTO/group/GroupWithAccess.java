package se.kth.DTO.group;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import se.kth.model.AccessLevel;
import se.kth.model.Group;

public record GroupWithAccess(
        UUID id,
        String name,
        String path,

        List<Group> subGroups,
        Map<String, List<String>> attributes,

        AccessLevel accessLevel) {

    public GroupWithAccess(Group group, AccessLevel accessLevel) {
        this(
                group.getId(),
                group.getName(),
                group.getPath(),
                group.getSubGroups(),
                group.getAttributes(),
                accessLevel);
    }

}
