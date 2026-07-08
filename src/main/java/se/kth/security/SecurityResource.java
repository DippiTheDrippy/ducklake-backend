package se.kth.security;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import io.quarkus.security.Authenticated;
import se.kth.dataset.dto.ListDatasetsResponse;
import se.kth.security.dto.CreateGroupRequest;
import se.kth.security.dto.GroupWithAccess;
import se.kth.security.dto.ListGroupsResponse;
import se.kth.security.dto.UpdatePermissionsRequest;
import se.kth.security.dto.UserWithAccess;
import se.kth.security.keycloak.Group;
import se.kth.security.keycloak.JwtUser;
import se.kth.security.keycloak.User;

@Slf4j
@Path("api/security")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class SecurityResource {

    private static final String ADMIN_ROLE = "admin";

    @Inject
    JsonWebToken jwt;

    @Inject
    SecurityService securityService;

    @GET
    @RolesAllowed(ADMIN_ROLE)
    @APIResponse(responseCode = "200", description = "List users", content = @Content(schema = @Schema(implementation = ListGroupsResponse.class)))
    public Response listUsers(
            @QueryParam("pageIndex") int pageIndex,
            @QueryParam("pageSize") int pageSize) {
        return Response.ok(securityService.listUsers(pageIndex, pageSize)).build();
    }

    @GET
    @Path("search")
    @APIResponse(responseCode = "200", description = "Search users", content = @Content(schema = @Schema(implementation = ListGroupsResponse.class)))
    public Response searchUsers(
            @QueryParam("search") String search,
            @QueryParam("pageIndex") int pageIndex,
            @QueryParam("pageSize") int pageSize) {
        return Response.ok(securityService.searchUsers(search, pageIndex, pageSize)).build();
    }

    @GET
    @Path("{id}")
    @APIResponse(responseCode = "200", description = "Get user", content = @Content(schema = @Schema(implementation = User.class)))
    public Response getUser(@PathParam("id") String id) {
        return Response.ok(securityService.getUser(id)).build();
    }

    @PUT
    @Path("{id}/dataset/{dataset_id}")
    @RolesAllowed(ADMIN_ROLE)
    public Response updateUserPermissions(@PathParam("id") String id,
            @PathParam("dataset_id") String datasetId,
            @Valid UpdatePermissionsRequest req) {
        securityService.updateUserPermissions(id, datasetId, req.accessLevel());
        return Response.noContent().build();
    }

    @DELETE
    @Path("{id}/dataset/{dataset_id}")
    @RolesAllowed(ADMIN_ROLE)
    public Response deleteUserPermissions(@PathParam("id") String userId,
            @PathParam("dataset_id") String datasetId) {
        securityService.deleteUserPermissions(userId, datasetId);
        return Response.noContent().build();
    }

    @GET
    @Path("users/dataset/{id}")
    @RolesAllowed(ADMIN_ROLE)
    @APIResponse(responseCode = "200", description = "List users with access to dataset", content = @Content(schema = @Schema(type = SchemaType.ARRAY, implementation = UserWithAccess.class)))
    public Response listUsersWithAccess(@PathParam("id") String datasetId) {
        return Response.ok(securityService.getUsersWithAccess(UUID.fromString(datasetId))).build();
    }

    /*
     * GROUPS
     */

    @GET
    @Path("groups")
    @RolesAllowed(ADMIN_ROLE)
    @APIResponse(responseCode = "200", description = "List groups", content = @Content(schema = @Schema(implementation = ListGroupsResponse.class)))
    public Response listGroups(
            @QueryParam("pageIndex") int pageIndex,
            @QueryParam("pageSize") int pageSize) {
        return Response.ok(securityService.listGroups(pageIndex, pageSize)).build();
    }

    @GET
    @Path("groups/me")
    @APIResponse(responseCode = "200", description = "List groups I belong to", content = @Content(schema = @Schema(implementation = ListGroupsResponse.class)))
    public Response listMyGroups(
            @QueryParam("pageIndex") int pageIndex,
            @QueryParam("pageSize") int pageSize) {
        JwtUser user = JwtUser.fromToken(jwt);

        return Response.ok(securityService.listMyGroups(user, pageIndex, pageSize)).build();
    }

    @GET
    @Path("groups/search")
    @APIResponse(responseCode = "200", description = "Search groups", content = @Content(schema = @Schema(implementation = ListGroupsResponse.class)))
    public Response searchGroups(
            @QueryParam("search") String search,
            @QueryParam("pageIndex") int pageIndex,
            @QueryParam("pageSize") int pageSize) {
        return Response.ok(securityService.searchGroups(search, pageIndex, pageSize)).build();
    }

    @GET
    @Path("groups/{id}")
    @APIResponse(responseCode = "200", description = "Get group", content = @Content(schema = @Schema(implementation = Group.class)))
    public Response getGroup(@PathParam("id") String id) {
        return Response.ok(securityService.getGroup(id)).build();
    }

    @GET
    @Path("groups/{id}/members")
    @APIResponse(responseCode = "200", description = "List group members", content = @Content(schema = @Schema(type = SchemaType.ARRAY, implementation = User.class)))
    public Response getGroupMembers(@PathParam("id") String id) {
        return Response.ok(securityService.getGroupMembers(id)).build();
    }

    @PUT
    @Path("groups/{id}/dataset/{dataset_id}")
    @RolesAllowed(ADMIN_ROLE)
    public Response updateGroupPermissions(@PathParam("id") String id,
            @PathParam("dataset_id") String datasetId,
            @Valid UpdatePermissionsRequest req) {
        securityService.updateGroupPermissions(id, datasetId, req.accessLevel());
        return Response.noContent().build();
    }

    @DELETE
    @Path("groups/{id}/dataset/{dataset_id}")
    @RolesAllowed(ADMIN_ROLE)
    public Response deleteGroupPermissions(@PathParam("id") String groupId,
            @PathParam("dataset_id") String datasetId) {
        securityService.deleteGroupPermissions(groupId, datasetId);
        return Response.noContent().build();
    }

    @GET
    @Path("groups/dataset/{id}")
    @RolesAllowed(ADMIN_ROLE)
    @APIResponse(responseCode = "200", description = "List groups with access to dataset", content = @Content(schema = @Schema(type = SchemaType.ARRAY, implementation = GroupWithAccess.class)))
    public Response listGroupsWithAccess(@PathParam("id") String datasetId) {
        return Response.ok(securityService.getGroupsWithAccess(UUID.fromString(datasetId))).build();
    }
}
