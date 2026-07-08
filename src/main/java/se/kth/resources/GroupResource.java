package se.kth.resources;

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
import se.kth.DTO.group.GroupWithAccess;
import se.kth.DTO.group.ListGroupsResponse;
import se.kth.model.Group;
import se.kth.model.JwtUser;
import se.kth.model.User;
import se.kth.services.GroupService;

@Slf4j
@Path("api/groups")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class GroupResource {

    private static final String ADMIN_ROLE = "admin";

    @Inject
    JsonWebToken jwt;

    @Inject
    GroupService groupService;

    @GET
    @Path("")
    @RolesAllowed(ADMIN_ROLE)
    @APIResponse(responseCode = "200", description = "List groups", content = @Content(schema = @Schema(implementation = ListGroupsResponse.class)))
    public Response listGroups(
            @QueryParam("pageIndex") int pageIndex,
            @QueryParam("pageSize") int pageSize) {
        return Response.ok(groupService.listGroups(pageIndex, pageSize)).build();
    }

    @GET
    @Path("me")
    @APIResponse(responseCode = "200", description = "List groups I belong to", content = @Content(schema = @Schema(implementation = ListGroupsResponse.class)))
    public Response listMyGroups(
            @QueryParam("pageIndex") int pageIndex,
            @QueryParam("pageSize") int pageSize) {
        JwtUser user = JwtUser.fromToken(jwt);

        return Response.ok(groupService.listMyGroups(user, pageIndex, pageSize)).build();
    }

    @GET
    @Path("search")
    @APIResponse(responseCode = "200", description = "Search groups", content = @Content(schema = @Schema(implementation = ListGroupsResponse.class)))
    public Response searchGroups(
            @QueryParam("search") String search,
            @QueryParam("pageIndex") int pageIndex,
            @QueryParam("pageSize") int pageSize) {
        return Response.ok(groupService.searchGroups(search, pageIndex, pageSize)).build();
    }

    @GET
    @Path("{id}")
    @APIResponse(responseCode = "200", description = "Get group", content = @Content(schema = @Schema(implementation = Group.class)))
    public Response getGroup(@PathParam("id") String id) {
        return Response.ok(groupService.getGroup(id)).build();
    }

    @GET
    @Path("{id}/members")
    @APIResponse(responseCode = "200", description = "List group members", content = @Content(schema = @Schema(type = SchemaType.ARRAY, implementation = User.class)))
    public Response getGroupMembers(@PathParam("id") String id) {
        return Response.ok(groupService.getGroupMembers(id)).build();
    }

    @PUT
    @Path("{id}/dataset/{dataset_id}")
    @RolesAllowed(ADMIN_ROLE)
    public Response updateGroupPermissions(@PathParam("id") String id,
            @PathParam("dataset_id") String datasetId,
            @Valid UpdatePermissionsRequest req) {
        groupService.updateGroupPermissions(id, datasetId, req.accessLevel());
        return Response.noContent().build();
    }

    @DELETE
    @Path("{id}/dataset/{dataset_id}")
    @RolesAllowed(ADMIN_ROLE)
    public Response deleteGroupPermissions(@PathParam("id") String groupId,
            @PathParam("dataset_id") String datasetId) {
        groupService.deleteGroupPermissions(groupId, datasetId);
        return Response.noContent().build();
    }

    @GET
    @Path("dataset/{id}")
    @RolesAllowed(ADMIN_ROLE)
    @APIResponse(responseCode = "200", description = "List groups with access to dataset", content = @Content(schema = @Schema(type = SchemaType.ARRAY, implementation = GroupWithAccess.class)))
    public Response listGroupsWithAccess(@PathParam("id") String datasetId) {
        return Response.ok(groupService.getGroupsWithAccess(UUID.fromString(datasetId))).build();
    }
}
