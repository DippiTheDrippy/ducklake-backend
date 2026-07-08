package se.kth.resources;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import io.quarkus.security.Authenticated;
import se.kth.DTO.UpdatePermissionsRequest;
import se.kth.DTO.group.ListGroupsResponse;
import se.kth.DTO.user.UserWithAccess;
import se.kth.model.User;
import se.kth.services.UserService;

@Slf4j
@Path("api/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class UserResource {

    private static final String ADMIN_ROLE = "admin";

    @Inject
    JsonWebToken jwt;

    @Inject
    UserService userService;

    @GET
    @RolesAllowed(ADMIN_ROLE)
    @APIResponse(responseCode = "200", description = "List users", content = @Content(schema = @Schema(implementation = ListGroupsResponse.class)))
    public Response listUsers(
            @QueryParam("pageIndex") int pageIndex,
            @QueryParam("pageSize") int pageSize) {
        return Response.ok(userService.listUsers(pageIndex, pageSize)).build();
    }

    @GET
    @Path("search")
    @APIResponse(responseCode = "200", description = "Search users", content = @Content(schema = @Schema(implementation = ListGroupsResponse.class)))
    public Response searchUsers(
            @QueryParam("search") String search,
            @QueryParam("pageIndex") int pageIndex,
            @QueryParam("pageSize") int pageSize) {
        return Response.ok(userService.searchUsers(search, pageIndex, pageSize)).build();
    }

    @GET
    @Path("{id}")
    @APIResponse(responseCode = "200", description = "Get user", content = @Content(schema = @Schema(implementation = User.class)))
    public Response getUser(@PathParam("id") String id) {
        return Response.ok(userService.getUser(id)).build();
    }

    @PUT
    @Path("{id}/dataset/{dataset_id}")
    @RolesAllowed(ADMIN_ROLE)
    public Response updateUserPermissions(@PathParam("id") String id,
            @PathParam("dataset_id") String datasetId,
            @Valid UpdatePermissionsRequest req) {
        userService.updateUserPermissions(id, datasetId, req.accessLevel());
        return Response.noContent().build();
    }

    @DELETE
    @Path("{id}/dataset/{dataset_id}")
    @RolesAllowed(ADMIN_ROLE)
    public Response deleteUserPermissions(@PathParam("id") String userId,
            @PathParam("dataset_id") String datasetId) {
        userService.deleteUserPermissions(userId, datasetId);
        return Response.noContent().build();
    }

    @GET
    @Path("dataset/{id}")
    @RolesAllowed(ADMIN_ROLE)
    @APIResponse(responseCode = "200", description = "List users with access to dataset", content = @Content(schema = @Schema(type = SchemaType.ARRAY, implementation = UserWithAccess.class)))
    public Response listUsersWithAccess(@PathParam("id") String datasetId) {
        return Response.ok(userService.getUsersWithAccess(UUID.fromString(datasetId))).build();
    }
}
