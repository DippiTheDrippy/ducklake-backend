package se.kth.dataset;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import io.quarkus.security.Authenticated;
import io.quarkus.security.UnauthorizedException;
import se.kth.credential.Credential;
import se.kth.credential.CredentialService;
import se.kth.dataset.dto.CreateCredentialRequest;
import se.kth.dataset.dto.CreateCredentialResponse;
import se.kth.dataset.dto.ListDatasetsResponse;
import se.kth.favorite.FavoriteService;
import se.kth.security.keycloak.JwtUser;

@Slf4j
@Path("api/datasets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class DatasetResource {

    private static final String ADMIN_ROLE = "admin";

    @Inject
    JsonWebToken jwt;

    @Inject
    DatasetService datasetService;

    @Inject
    CredentialService credentialService;

    @Inject
    FavoriteService favoriteService;

    @GET
    @APIResponse(responseCode = "200", description = "List accessible datasets", content = @Content(schema = @Schema(implementation = ListDatasetsResponse.class)))
    public Response listDatasets(@QueryParam("pageIndex") int pageIndex, @QueryParam("pageSize") int pageSize) {
        JwtUser user = JwtUser.fromToken(jwt);

        if (user.isInGroup(ADMIN_ROLE)) {
            return Response.ok(datasetService.listDatasets(pageIndex, pageSize)).build();
        } else {
            return Response.ok(datasetService.listUserDatasets(user, pageIndex, pageSize)).build();
        }
    }

    @GET
    @Path("{id}")
    @APIResponse(responseCode = "200", description = "Get dataset", content = @Content(schema = @Schema(implementation = Dataset.class)))
    public Response getDataset(@PathParam("id") String id) {
        JwtUser user = JwtUser.fromToken(jwt);

        if (user.isInGroup(ADMIN_ROLE)) {
            return Response.ok(datasetService.getDataset(id)).build();
        } else {
            return Response.ok(datasetService.getDatasetForUser(id, user)).build();
        }
    }

    @GET
    @Path("search")
    @APIResponse(responseCode = "200", description = "Search datasets by name, display name, or description", content = @Content(schema = @Schema(implementation = ListDatasetsResponse.class)))
    public Response searchDatasets(
            @QueryParam("search") String search,
            @QueryParam("pageIndex") int pageIndex,
            @QueryParam("pageSize") int pageSize) {
        JwtUser user = JwtUser.fromToken(jwt);

        if (user.isInGroup(ADMIN_ROLE)) {
            return Response.ok(datasetService.searchDatasets(search, pageIndex, pageSize)).build();
        } else {
            return Response.ok(datasetService.searchDatasetsForUser(search, user, pageIndex, pageSize)).build();
        }
    }

    /*
     * FAVORITES
     */

    @GET
    @Path("favorite")
    @APIResponse(responseCode = "200", description = "List favortied datasets", content = @Content(schema = @Schema(implementation = ListDatasetsResponse.class)))
    public Response listFavorites(
            @QueryParam("pageIndex") int pageIndex,
            @QueryParam("pageSize") int pageSize) {
        JwtUser user = JwtUser.fromToken(jwt);

        return Response.ok(favoriteService.listFavoritedDatasets(user, pageIndex, pageSize)).build();
    }

    @POST
    @Path("favorite/{id}")
    public Response favoriteDataset(@PathParam("id") String id) {
        JwtUser user = JwtUser.fromToken(jwt);

        favoriteService.addFavorite(user, id);
        return Response.noContent().build();
    }

    @DELETE
    @Path("favorite/{id}")
    public Response unfavoriteDataset(@PathParam("id") String id) {
        JwtUser user = JwtUser.fromToken(jwt);

        favoriteService.removeFavorite(user, id);
        return Response.noContent().build();
    }

    /*
     * DATASET CREDENTIALS
     */

    @GET
    @Path("credentials")
    @APIResponse(responseCode = "200", description = "List credentials", content = @Content(schema = @Schema(implementation = ListDatasetsResponse.class)))
    public Response listMyCredentials(
            @QueryParam("pageIndex") int pageIndex,
            @QueryParam("pageSize") int pageSize) {
        JwtUser user = JwtUser.fromToken(jwt);

        return Response.ok(credentialService.listUserCredentials(user, pageIndex, pageSize)).build();
    }

    @GET
    @Path("{id}/credentials")
    @APIResponse(responseCode = "200", description = "Get dataset credential", content = @Content(schema = @Schema(implementation = Credential.class)))
    public Response getDatasetCredential(@PathParam("id") String id) {
        JwtUser user = JwtUser.fromToken(jwt);

        return Response.ok(credentialService.getDatasetCredential(user, id)).build();
    }

    @POST
    @Path("{id}/credentials")
    @APIResponse(responseCode = "200", description = "Successfully created credentials", content = @Content(schema = @Schema(implementation = CreateCredentialResponse.class)))
    public Response createDatasetCredentials(
            @PathParam("id") String id,
            CreateCredentialRequest req) {
        JwtUser user = JwtUser.fromToken(jwt);

        return Response.ok(credentialService.createCredential(user, id, req)).build();
    }

    @POST
    @Path("credentials/{id}/rotate")
    @APIResponse(responseCode = "200", description = "Successfully rotated credentials", content = @Content(schema = @Schema(implementation = CreateCredentialResponse.class)))
    public Response rotateDatasetCredentials(@PathParam("id") String id) {
        JwtUser user = JwtUser.fromToken(jwt);

        return Response.ok(credentialService.rotateCredential(user, id)).build();
    }

    @DELETE
    @Path("credentials/{id}")
    public Response deleteCredentials(@PathParam("id") String id) {
        JwtUser user = JwtUser.fromToken(jwt);

        credentialService.deleteCredential(user, id);
        return Response.noContent().build();
    }

}
