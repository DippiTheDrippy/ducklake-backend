package se.kth.security.keycloak;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.oidc.client.filter.OidcClientFilter;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

@RegisterRestClient(configKey = "keycloak-admin-api")
@OidcClientFilter("keycloak-admin")
@Path("/admin/realms/{realm}")
public interface KeycloakAdminClient {

    @GET
    @Path("/users")
    List<User> getUsers(
            @PathParam("realm") String realm,
            @QueryParam("first") Integer first,
            @QueryParam("max") Integer max,
            @QueryParam("search") String search,
            @QueryParam("briefRepresentation") Boolean briefRepresentation);

    @GET
    @Path("/users/count")
    long countUsers(
            @PathParam("realm") String realm,
            @QueryParam("search") String search);

    @GET
    @Path("/users/{userId}")
    User getUserById(
            @PathParam("realm") String realm,
            @PathParam("userId") String userId);

    @GET
    @Path("/users/{userId}/groups")
    List<Group> getGroupsForUser(
            @PathParam("realm") String realm,
            @PathParam("userId") String userId,
            @QueryParam("first") Integer first,
            @QueryParam("max") Integer max,
            @QueryParam("search") String search,
            @QueryParam("briefRepresentation") Boolean briefRepresentation);

    @GET
    @Path("/users/{userId}/groups/count")
    Map<String, Long> countGroupsForUser(
            @PathParam("realm") String realm,
            @PathParam("userId") String userId,
            @QueryParam("search") String search);

    @GET
    @Path("/groups")
    List<Group> getGroups(
            @PathParam("realm") String realm,
            @QueryParam("first") Integer first,
            @QueryParam("max") Integer max,
            @QueryParam("search") String search,
            @QueryParam("briefRepresentation") Boolean briefRepresentation,
            @QueryParam("populateHierarchy") Boolean populateHierarchy,
            @QueryParam("subGroupsCount") Boolean subGroupsCount);

    @GET
    @Path("/groups/count")
    Map<String, Long> countGroups(
            @PathParam("realm") String realm,
            @QueryParam("search") String search);

    @GET
    @Path("/groups/{groupId}")
    Group getGroupById(
            @PathParam("realm") String realm,
            @PathParam("groupId") String groupId);

    @GET
    @Path("/groups/{groupId}/members")
    List<User> getGroupMembers(
            @PathParam("realm") String realm,
            @PathParam("groupId") String groupId,
            @QueryParam("first") Integer first,
            @QueryParam("max") Integer max,
            @QueryParam("briefRepresentation") Boolean briefRepresentation);
}