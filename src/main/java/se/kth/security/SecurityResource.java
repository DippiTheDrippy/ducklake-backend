package se.kth.security;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.Authenticated;
import se.kth.security.dto.CreateGroupRequest;
import se.kth.security.dto.UpdatePermissionsRequest;
import se.kth.security.keycloak.JwtUser;

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
    public Response listUsers(
            @QueryParam("pageIndex") int pageIndex,
            @QueryParam("pageSize") int pageSize) {
        try {
            return Response.ok(securityService.listUsers(pageIndex, pageSize)).build();
        } catch (NotFoundException e) {
            log.error(e.getMessage());
            return Response.serverError().entity(e.getMessage()).build();
        } catch (Exception e) {
            log.error(e.getMessage());
            return Response.serverError().build();
        }
    }

    @GET
    @Path("{id}")
    public Response getUser(@PathParam("id") String id) {
        JwtUser user = JwtUser.fromToken(jwt);

        try {
            if (user.isInGroup(ADMIN_ROLE)) {
                return Response.ok(securityService.getUser(id)).build();
            } else {
                return Response.ok(securityService.getMyself(user)).build();
            }
        } catch (NotFoundException e) {
            log.error(e.getMessage());
            return Response.serverError().entity(e.getMessage()).build();
        } catch (Exception e) {
            log.error(e.getMessage());
            return Response.serverError().build();
        }
    }

    @PUT
    @Path("{id}/dataset/{dataset_id}")
    @RolesAllowed(ADMIN_ROLE)
    public Response updateUserPermissions(@PathParam("id") String id,
            @PathParam("dataset_id") String datasetId,
            @Valid UpdatePermissionsRequest req) {
        try {
            securityService.updateUserPermissions(id, datasetId, req.accessLevel());
            return Response.ok().build();
        } catch (NotFoundException e) {
            log.error(e.getMessage());
            return Response.serverError().entity(e.getMessage()).build();
        } catch (Exception e) {
            log.error(e.getMessage());
            return Response.serverError().build();
        }
    }

    /*
     * GROUPS
     */

    @GET
    @Path("groups")
    public Response listGroups(
            @QueryParam("pageIndex") int pageIndex,
            @QueryParam("pageSize") int pageSize) {
        JwtUser user = JwtUser.fromToken(jwt);

        try {
            if (user.isInGroup(ADMIN_ROLE)) {
                return Response.ok(securityService.listGroups(pageIndex, pageSize)).build();
            } else {
                return Response.ok(securityService.listMyGroups(user, pageIndex, pageSize)).build();
            }
        } catch (NotFoundException e) {
            log.error(e.getMessage());
            return Response.serverError().entity(e.getMessage()).build();
        } catch (Exception e) {
            log.error(e.getMessage());
            return Response.serverError().build();
        }
    }

    @GET
    @Path("groups/{id}")
    public Response getGroup(@PathParam("id") String id) {
        try {
            return Response.ok(securityService.getGroup(id)).build();
        } catch (NotFoundException e) {
            log.error(e.getMessage());
            return Response.serverError().entity(e.getMessage()).build();
        } catch (Exception e) {
            log.error(e.getMessage());
            return Response.serverError().build();
        }
    }

    @PUT
    @Path("groups/{id}/dataset/{dataset_id}")
    @RolesAllowed(ADMIN_ROLE)
    public Response updateGroupPermissions(@PathParam("id") String id,
            @PathParam("dataset_id") String datasetId,
            @Valid UpdatePermissionsRequest req) {
        try {
            securityService.updateGroupPermissions(id, datasetId, req.accessLevel());
            return Response.ok().build();
        } catch (NotFoundException e) {
            log.error(e.getMessage());
            return Response.serverError().entity(e.getMessage()).build();
        } catch (Exception e) {
            log.error(e.getMessage());
            return Response.serverError().build();
        }
    }
}
