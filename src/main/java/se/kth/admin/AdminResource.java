package se.kth.admin;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("api/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminResource {

  @POST
  @Path("datasets")
  @PermitAll
  public Response createDataset() {
    return Response.noContent().build();
  }

  @PUT
  @Path("{id}")
  @PermitAll
  public Response updateDataset(@PathParam("id") String id) {
    return Response.noContent().build();
  }

  @DELETE
  @Path("{id}")
  @PermitAll
  public Response deleteDataset(@PathParam("id") String id) {
    return Response.noContent().build();
  }

}
