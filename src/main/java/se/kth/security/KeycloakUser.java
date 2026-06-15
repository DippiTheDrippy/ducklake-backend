package se.kth.security;

import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Set;

// Helper for extracting user from the keycloak token.
public record KeycloakUser(String firstName, String lastName, String email, Set<String> groups) {

    public static KeycloakUser fromToken(JsonWebToken jwt) {
        return new KeycloakUser(
                jwt.getClaim("given_name"),
                jwt.getClaim("family_name"),
                jwt.getClaim("email"),
                jwt.getGroups()
        );
    }

    public boolean isInGroup(String group) {
        return groups != null && groups.contains(group);
    }

}
