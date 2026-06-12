package se.kth.dataset;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("api/datasets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DatasetResource {

  @GET
  @PermitAll
  public Response listDatasets() {
    // Fetch datasets based on groups
    return Response.noContent().build();
  }

  @GET
  @Path("{id}")
  @PermitAll
  public Response getDataset(@PathParam("id") String id) {
    // include credentials if existing, otherwise its empty indicating to frontend no credentials
    return Response.noContent().build();
  }

  /*
   * For user-specific things like favorites and credentials
   * use the JWT token to extract unique identifier for user. 
   */

  @POST
  @Path("{id}")
  @PermitAll
  public Response favoriteDataset(@PathParam("id") String id) {
    return Response.noContent().build();
  }


  @POST
  @Path("{id}/credentials")
  @PermitAll
  public Response createCredentials(@PathParam("id") String id) {
    return Response.noContent().build();
  }

  @POST
  @Path("{id}/credentials/{credential_id}/rotate")
  @PermitAll
  public Response rotateCredentials(@PathParam("id") String id, @PathParam("credential_id") String credentialId) {
    return Response.noContent().build();
  }

  @POST
  @Path("{id}/credentials/{credential_id}")
  @PermitAll
  public Response deleteCredentials(@PathParam("id") String id, @PathParam("credential_id") String credentialId) {
    return Response.noContent().build();
  }

}
