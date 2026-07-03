package se.kth.dataset;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.Authenticated;
import io.quarkus.security.UnauthorizedException;
import se.kth.credential.CredentialService;
import se.kth.dataset.dto.CreateCredentialRequest;
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
    public Response listDatasets(@QueryParam("pageIndex") int pageIndex, @QueryParam("pageSize") int pageSize) {
        JwtUser user = JwtUser.fromToken(jwt);

        try {
            if (user.isInGroup(ADMIN_ROLE)) {
                return Response.ok(datasetService.listDatasets(pageIndex, pageSize)).build();
            } else {
                return Response.ok(datasetService.listUserDatasets(user, pageIndex, pageSize)).build();
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return Response.serverError().build();
        }
    }

    @GET
    @Path("{id}")
    public Response getDataset(@PathParam("id") String id) {
        JwtUser user = JwtUser.fromToken(jwt);

        try {
            if (user.isInGroup(ADMIN_ROLE)) {
                return Response.ok(datasetService.getDataset(id)).build();
            } else {
                return Response.ok(datasetService.getDatasetForUser(id, user)).build();
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return Response.serverError().build();
        }
    }

    @GET
    @Path("search")
    public Response searchDatasets(
            @QueryParam("search") String search,
            @QueryParam("pageIndex") int pageIndex,
            @QueryParam("pageSize") int pageSize) {
        JwtUser user = JwtUser.fromToken(jwt);

        try {
            if (user.isInGroup(ADMIN_ROLE)) {
                return Response.ok(datasetService.searchDatasets(search, pageIndex, pageSize)).build();
            } else {
                return Response.ok(datasetService.searchDatasetsForUser(search, user, pageIndex, pageSize)).build();
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return Response.serverError().build();
        }
    }

    /*
     * FAVORITES
     */

    @GET
    @Path("favorite")
    public Response listFavorites(
            @QueryParam("pageIndex") int pageIndex,
            @QueryParam("pageSize") int pageSize) {
        JwtUser user = JwtUser.fromToken(jwt);

        try {
            return Response.ok(favoriteService.listFavoritedDatasets(user, pageIndex, pageSize)).build();
        } catch (BadRequestException e) {
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            return Response.serverError().build();
        }
    }

    @POST
    @Path("favorite/{id}")
    public Response favoriteDataset(@PathParam("id") String id) {
        JwtUser user = JwtUser.fromToken(jwt);

        try {
            favoriteService.addFavorite(user, id);
            return Response.ok().build();
        } catch (BadRequestException e) {
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            return Response.serverError().build();
        }
    }

    @DELETE
    @Path("favorite/{id}")
    public Response unfavoriteDataset(@PathParam("id") String id) {
        JwtUser user = JwtUser.fromToken(jwt);

        try {
            favoriteService.removeFavorite(user, id);
            return Response.ok().build();
        } catch (BadRequestException e) {
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            return Response.serverError().build();
        }
    }

    /*
     * DATASET CREDENTIALS
     */

    @GET
    @Path("credentials")
    public Response listMyCredentials(
            @QueryParam("pageIndex") int pageIndex,
            @QueryParam("pageSize") int pageSize) {
        JwtUser user = JwtUser.fromToken(jwt);

        try {
            return Response.ok(credentialService.listUserCredentials(user, pageIndex, pageSize)).build();
        } catch (Exception e) {
            log.error(e.getMessage());
            return Response.serverError().build();
        }
    }

    @GET
    @Path("{id}/credentials")
    public Response getDatasetCredential(@PathParam("id") String id) {
        JwtUser user = JwtUser.fromToken(jwt);

        try {
            return Response.ok(credentialService.getDatasetCredential(user, id)).build();
        } catch (Exception e) {
            log.error(e.getMessage());
            return Response.serverError().build();
        }
    }

    @POST
    @Path("{id}/credentials")
    public Response createDatasetCredentials(
            @PathParam("id") String id,
            CreateCredentialRequest req) {
        JwtUser user = JwtUser.fromToken(jwt);

        try {
            credentialService.createCredential(user, id, req);
            return Response.ok().build();
        } catch (UnauthorizedException e) {
            return Response.status(Status.UNAUTHORIZED).entity(e.getMessage()).build();
        } catch (Exception e) {
            log.error(e.getMessage());
            return Response.serverError().build();
        }
    }

    @POST
    @Path("credentials/{id}/rotate")
    public Response rotateDatasetCredentials(@PathParam("id") String id) {
        JwtUser user = JwtUser.fromToken(jwt);

        try {

            return Response.ok(credentialService.rotateCredential(user, id)).build();
        } catch (BadRequestException e) {
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (UnauthorizedException e) {
            return Response.status(Status.UNAUTHORIZED).entity(e.getMessage()).build();
        } catch (Exception e) {
            log.error(e.getMessage());
            return Response.serverError().build();
        }
    }

    @DELETE
    @Path("credentials/{id}")
    public Response deleteCredentials(@PathParam("id") String id) {
        JwtUser user = JwtUser.fromToken(jwt);

        try {
            credentialService.deleteCredential(user, id);
            return Response.ok().build();
        } catch (UnauthorizedException e) {
            return Response.status(Status.UNAUTHORIZED).entity(e.getMessage()).build();
        } catch (Exception e) {
            log.error(e.getMessage());
            return Response.serverError().build();
        }
    }

}
