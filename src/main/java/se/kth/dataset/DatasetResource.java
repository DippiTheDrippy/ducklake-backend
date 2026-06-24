package se.kth.dataset;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.Authenticated;
import se.kth.credential.CredentialService;
import se.kth.security.KeycloakUser;

@Slf4j
@Path("api/datasets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class DatasetResource {

    @Inject
    JsonWebToken jwt;

    @Inject
    DatasetService datasetService;

    @Inject
    CredentialService credentialService;

    @GET
    public Response listDatasets() {
        KeycloakUser user = KeycloakUser.fromToken(jwt);

        try {
            if (user.isInGroup("admin")) {
                return Response.ok(datasetService.listDatasets()).build();
            } else {
                return Response.ok(datasetService.listUserDatasets(user)).build();
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return Response.serverError().build();
        }
    }

    @GET
    @Path("{id}")
    public Response getDataset(@PathParam("id") String id) {
        KeycloakUser user = KeycloakUser.fromToken(jwt);

        try {
            if (user.isInGroup("admin")) {
                return Response.ok(datasetService.getDataset(id)).build();
            } else {
                return Response.ok(datasetService.getDatasetForUser(id, user)).build();
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return Response.serverError().build();
        }
    }

    /*
     * For user-specific things like favorites and credentials
     * use the JWT token to extract unique identifier for user.
     */

    @POST
    @Path("{id}")
    public Response favoriteDataset(@PathParam("id") String id) {
        return Response.noContent().build();
    }

    @GET
    @Path("{id}/credentials")
    public Response getCredentials(@PathParam("id") String id) {
        return Response.noContent().build();
    }

    @POST
    @Path("{id}/credentials")
    public Response createCredentials(@PathParam("id") String id) {
        return Response.noContent().build();
    }

    @POST
    @Path("{id}/credentials/{credential_id}/rotate")
    public Response rotateCredentials(@PathParam("id") String id, @PathParam("credential_id") String credentialId) {
        return Response.noContent().build();
    }

    @POST
    @Path("{id}/credentials/{credential_id}")
    public Response deleteCredentials(@PathParam("id") String id, @PathParam("credential_id") String credentialId) {
        return Response.noContent().build();
    }

}
