package se.kth.security.keycloak;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class User {

    private String id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private Boolean enabled;
    private Boolean emailVerified;

    private Map<String, List<String>> attributes;
}