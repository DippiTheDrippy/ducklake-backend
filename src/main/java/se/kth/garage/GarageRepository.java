package se.kth.garage;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import se.kth.garage.dto.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.InputStream;
import java.time.OffsetDateTime;

@ApplicationScoped
public class GarageRepository {

    private final S3Client s3Client;
    private final GarageAdminClient garageAdminClient;
    private final String adminToken;

    public GarageRepository(
            S3Client s3Client,
            @RestClient GarageAdminClient garageAdminClient,
            @ConfigProperty(name = "app.garage.admin.token") String adminToken) {
        this.s3Client = s3Client;
        this.garageAdminClient = garageAdminClient;
        this.adminToken = adminToken;
    }

    /*
     * BUCKETS
     */

    public CreateBucketResponse createBucket(String bucketName) {
        return garageAdminClient.createBucket(
                authorizationHeader(),
                new CreateBucketRequest(bucketName));
    }

    public CreateBucketResponse getBucketByGlobalAlias(String bucketName) {
        return garageAdminClient.getBucketInfo(
                authorizationHeader(),
                bucketName);
    }

    public void deleteBucketById(String bucketId) {
        garageAdminClient.deleteBucket(
                authorizationHeader(),
                bucketId);
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
                        neverExpires));
    }

    public void deleteKey(String keyId) {
        garageAdminClient.deleteKey(
                authorizationHeader(),
                keyId);
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
                                writeAccess)));
    }

    /*
     * UPLOADS
     */

    public void upload(String fileName, InputStream inputStream, long contentLength, String contentType,
            String bucketName) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(contentType)
                .build();

        s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
    }

    public void deleteFile(String bucket, String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        s3Client.deleteObject(request);
    }

    /*
     * HELPERS
     */

    private String authorizationHeader() {
        return "Bearer " + adminToken;
    }
}