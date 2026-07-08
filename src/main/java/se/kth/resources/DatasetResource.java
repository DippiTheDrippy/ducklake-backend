package se.kth.resources;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.RestForm;

import io.quarkus.security.Authenticated;
import se.kth.DTO.dataset.CreateDatasetMultipartRequest;
import se.kth.DTO.dataset.DatasetWithSummary;
import se.kth.DTO.dataset.ListDatasetsResponse;
import se.kth.DTO.dataset.UpdateDatasetRequest;
import se.kth.model.Dataset;
import se.kth.model.JwtUser;

import org.jboss.resteasy.reactive.multipart.FileUpload;

import se.kth.services.DatasetAdminService;
import se.kth.services.DatasetService;

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
    DatasetAdminService datasetAdminService;

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
    @APIResponse(responseCode = "200", description = "Get dataset", content = @Content(schema = @Schema(implementation = DatasetWithSummary.class)))
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

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RolesAllowed(ADMIN_ROLE)
    @APIResponse(responseCode = "200", description = "Successfully created dataset", content = @Content(schema = @Schema(implementation = Dataset.class)))
    public Response createDatasetFromFile(
            CreateDatasetMultipartRequest req) {
        return Response.ok(datasetAdminService.createDatasetFromFile(req.metadata(), req.file())).build();
    }

    @POST
    @Path("append/{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RolesAllowed(ADMIN_ROLE)
    public Response appendDataFromFile(
            @PathParam("id") String id,
            @RestForm("file") FileUpload file) {
        datasetAdminService.appendDataToDataset(id, file);
        return Response.noContent().build();
    }

    @PUT
    @Path("{id}")
    @RolesAllowed(ADMIN_ROLE)
    public Response updateDataset(@PathParam("id") String id,
            UpdateDatasetRequest req) {
        datasetAdminService.updateDataset(id, req);
        return Response.noContent().build();
    }

    @DELETE
    @Path("{id}")
    @RolesAllowed(ADMIN_ROLE)
    public Response deleteDataset(@PathParam("id") String id) {
        datasetAdminService.deleteDataset(id);
        return Response.noContent().build();
    }

}
