package se.kth.security.keycloak;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class Group {

    private String id;
    private String name;
    private String path;

    private List<Group> subGroups;

    private Map<String, List<String>> attributes;
}