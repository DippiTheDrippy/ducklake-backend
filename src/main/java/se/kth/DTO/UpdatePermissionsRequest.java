package se.kth.DTO;

import jakarta.validation.constraints.NotNull;
import se.kth.model.AccessLevel;

public record UpdatePermissionsRequest(
        @NotNull AccessLevel accessLevel
) {
}
