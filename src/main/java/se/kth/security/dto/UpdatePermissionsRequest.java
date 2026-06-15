package se.kth.security.dto;

import jakarta.validation.constraints.NotNull;
import se.kth.security.AccessLevel;

public record UpdatePermissionsRequest(
        @NotNull AccessLevel accessLevel
) {
}
