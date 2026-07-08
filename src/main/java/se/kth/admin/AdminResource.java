package se.kth.admin;

import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import se.kth.admin.dto.CreateDatasetMultipartRequest;
import se.kth.admin.dto.CreateDatasetRequest;
import se.kth.admin.dto.UpdateDatasetRequest;
import se.kth.dataset.Dataset;
import se.kth.security.keycloak.JwtUser;

@Slf4j
@Path("api/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class AdminResource {

    private static final String ADMIN_ROLE = "admin";

    @Inject
    JsonWebToken jwt;

    @Inject
    AdminService adminService;

    // @POST
    // @Path("datasets")
    // @RolesAllowed(ADMIN_ROLE)
    // public Response createEmptyDataset(CreateDatasetRequest req) {
    // try {
    // return Response.ok(adminService.createEmptyDataset(req)).build();
    // } catch (Exception e) {
    // e.printStackTrace();
    // log.error(e.getMessage());
    // return Response.serverError().build();
    // }
    // }

    @POST
    @Path("datasets/file")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RolesAllowed(ADMIN_ROLE)
    @APIResponse(responseCode = "200", description = "Successfully created dataset", content = @Content(schema = @Schema(implementation = Dataset.class)))
    public Response createDatasetFromFile(
            CreateDatasetMultipartRequest req) {
        return Response.ok(adminService.createDatasetFromFile(req.metadata(), req.file())).build();
    }

    @POST
    @Path("datasets/append/{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RolesAllowed(ADMIN_ROLE)
    public Response appendDataFromFile(
            @PathParam("id") String id,
            @RestForm("file") FileUpload file) {
        adminService.appendDataToDataset(id, file);
        return Response.noContent().build();
    }

    @PUT
    @Path("datasets/{id}")
    @RolesAllowed(ADMIN_ROLE)
    public Response updateDataset(@PathParam("id") String id,
            UpdateDatasetRequest req) {
        adminService.updateDataset(id, req);
        return Response.noContent().build();
    }

    @DELETE
    @Path("datasets/{id}")
    @RolesAllowed(ADMIN_ROLE)
    public Response deleteDataset(@PathParam("id") String id) {
        adminService.deleteDataset(id);
        return Response.noContent().build();
    }

}
