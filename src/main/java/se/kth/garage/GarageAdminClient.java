package se.kth.garage;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import se.kth.garage.dto.*;

@Path("/v2")
@RegisterRestClient(configKey = "garage-admin")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface GarageAdminClient {

        /*
         * BUCKETS
         */

        @POST
        @Path("/CreateBucket")
        CreateBucketResponse createBucket(
                        @HeaderParam("Authorization") String authorization,
                        CreateBucketRequest request);

        @GET
        @Path("/GetBucketInfo")
        CreateBucketResponse getBucketInfo(
                        @HeaderParam("Authorization") String authorization,
                        @QueryParam("globalAlias") String globalAlias);

        @POST
        @Path("/DeleteBucket")
        void deleteBucket(
                        @HeaderParam("Authorization") String authorization,
                        @QueryParam("id") String bucketId);

        /*
         * ACCESS KEYS
         */

        @POST
        @Path("/CreateKey")
        CreateKeyResponse createKey(
                        @HeaderParam("Authorization") String authorization,
                        CreateKeyRequest request);

        @POST
        @Path("/DeleteKey")
        void deleteKey(
                        @HeaderParam("Authorization") String authorization,
                        @QueryParam("id") String keyId);

        @POST
        @Path("/AllowBucketKey")
        void allowBucketKey(
                        @HeaderParam("Authorization") String authorization,
                        AllowKeyRequest request);
}