package se.kth.security.keycloak;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.Data;

@Data
public class Group {

    private UUID id;
    private String name;
    private String path;

    private List<Group> subGroups;

    private Map<String, List<String>> attributes;
}