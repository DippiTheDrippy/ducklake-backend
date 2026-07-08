package se.kth.model;

import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Set;
import java.util.UUID;

// Helper for extracting user from the keycloak token.
public record JwtUser(UUID id, String firstName, String lastName, String email, Set<String> groups) {

    public static JwtUser fromToken(JsonWebToken jwt) {
        return new JwtUser(
                UUID.fromString(jwt.getSubject()),
                jwt.getClaim("given_name"),
                jwt.getClaim("family_name"),
                jwt.getClaim("email"),
                jwt.getGroups());
    }

    public boolean isInGroup(String group) {
        return groups != null && groups.contains(group);
    }

}
