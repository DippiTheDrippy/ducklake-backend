package se.kth.garage;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import se.kth.garage.dto.*;

import java.time.OffsetDateTime;

@ApplicationScoped
public class GarageRepository {

    private final GarageAdminClient garageAdminClient;
    private final String adminToken;

    public GarageRepository(
            @RestClient GarageAdminClient garageAdminClient,
            @ConfigProperty(name = "app.garage.admin.token") String adminToken
    ) {
        this.garageAdminClient = garageAdminClient;
        this.adminToken = adminToken;
    }

    /*
     * BUCKETS
     */

    public CreateBucketResponse createBucket(String bucketName) {
        return garageAdminClient.createBucket(
                authorizationHeader(),
                new CreateBucketRequest(bucketName)
        );
    }

    public CreateBucketResponse getBucketByGlobalAlias(String bucketName) {
        return garageAdminClient.getBucketInfo(
                authorizationHeader(),
                bucketName
        );
    }

    public void deleteBucketById(String bucketId) {
        garageAdminClient.deleteBucket(
                authorizationHeader(),
                bucketId
        );
    }

    public void deleteBucketByGlobalAlias(String bucketName) {
        CreateBucketResponse bucket = getBucketByGlobalAlias(bucketName);
        deleteBucketById(bucket.id());
    }

    /*
     * ACCESS KEYS
     */

    public CreateKeyResponse createKey(String name, OffsetDateTime expiration, boolean neverExpires) {
        return garageAdminClient.createKey(
                authorizationHeader(),
                new CreateKeyRequest(
                        expiration,
                        name,
                        neverExpires
                )
        );
    }

    public void deleteKey(String keyId) {
        garageAdminClient.deleteKey(
                authorizationHeader(),
                keyId
        );
    }

    public void allowKey(String accessKeyId, String bucketId, boolean writeAccess) {
        garageAdminClient.allowBucketKey(
                authorizationHeader(),
                new AllowKeyRequest(
                        accessKeyId,
                        bucketId,
                        new KeyPermissions(
                                false,
                                true,
                                writeAccess
                        )
                )
        );
    }

    /*
     * HELPERS
     */

    private String authorizationHeader() {
        return "Bearer " + adminToken;
    }
}