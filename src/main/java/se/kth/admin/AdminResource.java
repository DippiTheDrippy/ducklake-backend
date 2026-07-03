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

    @POST
    @Path("datasets")
    @RolesAllowed(ADMIN_ROLE)
    public Response createEmptyDataset(CreateDatasetRequest req) {
        try {
            return Response.ok(adminService.createEmptyDataset(req)).build();
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            return Response.serverError().build();
        }
    }

    @POST
    @Path("datasets/file")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RolesAllowed(ADMIN_ROLE)
    public Response createDatasetFromFile(
            CreateDatasetMultipartRequest req) {
        try {
            return Response.ok(adminService.createDatasetFromFile(req.metadata(), req.file())).build();
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            return Response.serverError().build();
        }
    }

    @POST
    @Path("datasets/append/{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RolesAllowed(ADMIN_ROLE)
    public Response appendDataFromFile(
            @PathParam("id") String id,
            @RestForm("file") FileUpload file) {
        try {
            adminService.appendDataToDataset(id, file);
            return Response.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            return Response.serverError().build();
        }
    }

    @PUT
    @Path("datasets/{id}")
    @RolesAllowed(ADMIN_ROLE)
    public Response updateDataset(@PathParam("id") String id,
            UpdateDatasetRequest req) {
        try {
            adminService.updateDataset(id, req);
            return Response.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            return Response.serverError().build();
        }
    }

    @DELETE
    @Path("datasets/{id}")
    @RolesAllowed(ADMIN_ROLE)
    public Response deleteDataset(@PathParam("id") String id) {
        try {
            adminService.deleteDataset(id);
            return Response.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            return Response.serverError().build();
        }
    }

}
