package se.kth.resources;

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
import se.kth.DTO.credentials.CreateCredentialRequest;
import se.kth.DTO.credentials.CreateCredentialResponse;
import se.kth.DTO.dataset.ListDatasetsResponse;
import se.kth.model.Credential;
import se.kth.model.JwtUser;
import se.kth.services.CredentialService;
import se.kth.services.DatasetService;
import se.kth.services.FavoriteService;

@Slf4j
@Path("api/favorites")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class FavoritesResource {

    @Inject
    JsonWebToken jwt;

    @Inject
    FavoriteService favoriteService;

    @GET
    @APIResponse(responseCode = "200", description = "List favortied datasets", content = @Content(schema = @Schema(implementation = ListDatasetsResponse.class)))
    public Response listFavorites(
            @QueryParam("pageIndex") int pageIndex,
            @QueryParam("pageSize") int pageSize) {
        JwtUser user = JwtUser.fromToken(jwt);

        return Response.ok(favoriteService.listFavoritedDatasets(user, pageIndex, pageSize)).build();
    }

    @POST
    @Path("{id}")
    public Response favoriteDataset(@PathParam("id") String id) {
        JwtUser user = JwtUser.fromToken(jwt);

        favoriteService.addFavorite(user, id);
        return Response.noContent().build();
    }

    @DELETE
    @Path("{id}")
    public Response unfavoriteDataset(@PathParam("id") String id) {
        JwtUser user = JwtUser.fromToken(jwt);

        favoriteService.removeFavorite(user, id);
        return Response.noContent().build();
    }
}
