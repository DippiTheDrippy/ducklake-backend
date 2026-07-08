package se.kth.DTO.user;

import java.util.List;
import java.util.Map;

import se.kth.model.AccessLevel;
import se.kth.model.User;

public record UserWithAccess(
        String id,
        String username,
        String firstName,
        String lastName,
        String email,
        Boolean enabled,
        Boolean emailVerified,

        Map<String, List<String>> attributes,

        AccessLevel accessLevel) {

    public UserWithAccess(User user, AccessLevel accessLevel) {
        this(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getEnabled(),
                user.getEmailVerified(),
                user.getAttributes(),
                accessLevel);
    }
}
