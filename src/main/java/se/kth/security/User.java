package se.kth.security;

import java.util.Set;

import org.eclipse.microprofile.jwt.JsonWebToken;

// Helper for extracting user from the keycloak token.
public record User(String firstName, String lastName, String email, String personalNr, Set<String> groups) {
  
  public static User fromToken(JsonWebToken jwt) {
        return new User(
                jwt.getClaim("given_name"),
                jwt.getClaim("family_name"),
                jwt.getClaim("email"),
                jwt.getClaim("personalNr"),
                jwt.getGroups()
        );
    }

    public boolean isInGroup(String group) {
        return groups != null && groups.contains(group);
    }

}
