package se.kth.repositories;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import se.kth.DTO.garage.AllowKeyRequest;
import se.kth.DTO.garage.CreateBucketRequest;
import se.kth.DTO.garage.CreateBucketResponse;
import se.kth.DTO.garage.CreateKeyRequest;
import se.kth.DTO.garage.CreateKeyResponse;
import se.kth.DTO.garage.KeyPermissions;
import se.kth.interfaces.GarageAdminClient;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.InputStream;
import java.nio.file.Path;
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
                        name,
                        expiration,
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

    public void upload(
            String fileName,
            Path path,
            String contentType,
            String bucketName,
            String accessKeyId,
            String secretAccessKey) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(contentType)
                .overrideConfiguration(config -> config.credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKeyId, secretAccessKey))))
                .build();

        s3Client.putObject(request, RequestBody.fromFile(path));
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