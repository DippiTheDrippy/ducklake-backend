package se.kth.security;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.Authenticated;
import se.kth.security.dto.CreateGroupRequest;
import se.kth.security.dto.UpdatePermissionsRequest;

@Slf4j
@Path("api/security")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class SecurityResource {

    @Inject
    JsonWebToken jwt;

    @Inject
    SecurityService securityService;

    @GET
    public Response listUsers() {
        KeycloakUser user = KeycloakUser.fromToken(jwt);

        try {
            if (user.isInGroup("admin")) {
                return Response.ok(securityService.listUsers()).build();
            } else {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return Response.serverError().build();
        }
    }

    @GET
    @Path("{id}")
    public Response getUser(@PathParam("id") String id) {
        KeycloakUser user = KeycloakUser.fromToken(jwt);

        try {
            if (user.isInGroup("admin")) {
                return Response.ok(securityService.getUser(id)).build();
            } else {
                return Response.ok(securityService.getMyself(user)).build();
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return Response.serverError().build();
        }
    }

    @POST
    @Path("register")
    public Response register() {
        KeycloakUser user = KeycloakUser.fromToken(jwt);

        try {
            securityService.register(user);
        } catch (Exception e) {
            log.error(e.getMessage());
            return Response.serverError().build();
        }

        return Response.ok().build();
    }

    @PUT
    @Path("{id}/dataset/{dataset_id}")
    public Response updateUserPermissions(@PathParam("id") String id,
            @PathParam("dataset_id") String datasetId,
            @Valid UpdatePermissionsRequest req) {
        KeycloakUser user = KeycloakUser.fromToken(jwt);

        try {
            if (user.isInGroup("admin")) {
                securityService.updateUserPermissions(id, datasetId, req.accessLevel());
                return Response.ok().build();
            } else {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return Response.serverError().build();
        }
    }

    @DELETE
    @Path("{id}")
    public Response deleteUser(@PathParam("id") String id) {
        KeycloakUser user = KeycloakUser.fromToken(jwt);

        try {
            if (user.isInGroup("admin")) {
                securityService.deleteUser(id);
                return Response.ok().build();
            } else {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
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
    public Response listGroups() {
        KeycloakUser user = KeycloakUser.fromToken(jwt);

        try {
            if (user.isInGroup("admin")) {
                return Response.ok(securityService.listGroups()).build();
            } else {
                return Response.ok(securityService.listMyGroups(user)).build();
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return Response.serverError().build();
        }
    }

    @GET
    @Path("groups/{id}")
    public Response getGroup(@PathParam("id") String id) {
        KeycloakUser user = KeycloakUser.fromToken(jwt);

        try {
            if (user.isInGroup("admin")) {
                return Response.ok(securityService.getGroup(id)).build();
            } else {
                return Response.ok(securityService.getGroupIfMember(id, user)).build();
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return Response.serverError().build();
        }
    }

    @POST
    @Path("groups")
    public Response createGroup(@Valid CreateGroupRequest req) {
        KeycloakUser user = KeycloakUser.fromToken(jwt);

        try {
            if (user.isInGroup("admin")) {
                return Response.ok(securityService.createGroup(req)).build();
            } else {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return Response.serverError().build();
        }
    }

    @PUT
    @Path("groups/{id}/dataset/{dataset_id}")
    public Response updateGroupPermissions(@PathParam("id") String id,
            @PathParam("dataset_id") String datasetId,
            @Valid UpdatePermissionsRequest req) {
        KeycloakUser user = KeycloakUser.fromToken(jwt);

        try {
            if (user.isInGroup("admin")) {
                securityService.updateGroupPermissions(id, datasetId, req.accessLevel());
                return Response.ok().build();
            } else {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return Response.serverError().build();
        }
    }

    @DELETE
    @Path("groups/{id}")
    public Response deleteGroups(@PathParam("id") String id) {
        KeycloakUser user = KeycloakUser.fromToken(jwt);

        try {
            if (user.isInGroup("admin")) {
                securityService.deleteGroup(id);
                return Response.ok().build();
            } else {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return Response.serverError().build();
        }
    }

}
