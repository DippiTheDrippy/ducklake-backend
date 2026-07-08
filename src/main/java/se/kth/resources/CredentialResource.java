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
@Path("api/credentials")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class CredentialResource {

    @Inject
    JsonWebToken jwt;

    @Inject
    CredentialService credentialService;

    @GET
    @APIResponse(responseCode = "200", description = "List credentials", content = @Content(schema = @Schema(implementation = ListDatasetsResponse.class)))
    public Response listMyCredentials(
            @QueryParam("pageIndex") int pageIndex,
            @QueryParam("pageSize") int pageSize) {
        JwtUser user = JwtUser.fromToken(jwt);

        return Response.ok(credentialService.listUserCredentials(user, pageIndex, pageSize)).build();
    }

    @GET
    @Path("dataset/{id}")
    @APIResponse(responseCode = "200", description = "Get dataset credential", content = @Content(schema = @Schema(implementation = Credential.class)))
    public Response getDatasetCredential(@PathParam("id") String id) {
        JwtUser user = JwtUser.fromToken(jwt);

        return Response.ok(credentialService.getDatasetCredential(user, id)).build();
    }

    @POST
    @Path("dataset/{id}")
    @APIResponse(responseCode = "200", description = "Successfully created credentials", content = @Content(schema = @Schema(implementation = CreateCredentialResponse.class)))
    public Response createDatasetCredentials(
            @PathParam("id") String id,
            CreateCredentialRequest req) {
        JwtUser user = JwtUser.fromToken(jwt);

        return Response.ok(credentialService.createCredential(user, id, req)).build();
    }

    @POST
    @Path("dateset/{id}/rotate")
    @APIResponse(responseCode = "200", description = "Successfully rotated credentials", content = @Content(schema = @Schema(implementation = CreateCredentialResponse.class)))
    public Response rotateDatasetCredentials(@PathParam("id") String id) {
        JwtUser user = JwtUser.fromToken(jwt);

        return Response.ok(credentialService.rotateCredential(user, id)).build();
    }

    @DELETE
    @Path("{id}")
    public Response deleteCredentials(@PathParam("id") String id) {
        JwtUser user = JwtUser.fromToken(jwt);

        credentialService.deleteCredential(user, id);
        return Response.noContent().build();
    }

}
