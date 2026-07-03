package se.kth.admin;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import se.kth.admin.dto.CreateDatasetRequest;
import se.kth.admin.dto.UpdateDatasetRequest;
import se.kth.common.exceptions.DatasetAlreadyExistsException;
import se.kth.common.exceptions.DatasetCreationException;
import se.kth.common.exceptions.DatasetDeletionException;
import se.kth.common.exceptions.DatasetInsertionException;
import se.kth.credential.Credential;
import se.kth.credential.CredentialRepository;
import se.kth.dataset.Dataset;
import se.kth.dataset.DatasetRepository;
import se.kth.ducklake.DucklakeRepository;
import se.kth.ducklake.DucklakeRepository.ConnectionArgs;
import se.kth.garage.GarageRepository;
import se.kth.garage.dto.CreateBucketResponse;
import se.kth.garage.dto.CreateKeyResponse;
import se.kth.postgres.PostgresAdminRepository;
import se.kth.postgres.PostgresUserRepository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.multipart.FileUpload;

@Slf4j
@ApplicationScoped
public class AdminService {

    @Inject
    DatasetRepository datasetRepository;

    @Inject
    DucklakeRepository ducklakeRepository;

    @Inject
    GarageRepository garageRepository;

    @Inject
    CredentialRepository credentialRepository;

    @Inject
    PostgresAdminRepository postgresAdminRepository;

    @Inject
    PostgresUserRepository postgresUserRepository;

    @ConfigProperty(name = "app.ducklake.local.enabled", defaultValue = "false")
    boolean localModeEnabled;

    @ConfigProperty(name = "app.ducklake.local.path", defaultValue = "ducklake_data/")
    String localDataPath;

    /*
     * Will only create an empty bucket and an empty postgres database, it will be
     * up to the user to make sure that manually created dataset table has the same
     * name as the one specified in the CreateDatasetRequest.
     */
    public Dataset createEmptyDataset(CreateDatasetRequest req) {
        if (!datasetRepository.findByName(req.name()).isEmpty()) {
            throw new DatasetAlreadyExistsException("Dataset with that name already exists", null);
        }

        String bucketName = "ducklake-cbh-" + UUID.randomUUID().toString();

        CreateBucketResponse bucket = null;

        try {
            // Create postgres db
            postgresAdminRepository.createDatabase(req.name());

            // Create bucket
            bucket = garageRepository.createBucket(bucketName);

            // Persist dataset info
            Dataset d = new Dataset(
                    req.name(),
                    req.displayName(),
                    req.description(),
                    bucketName,
                    req.isPublic());

            return datasetRepository.save(d);

        } catch (Exception e) {
            // Clean up bucket and database if dataset was unable to be created
            if (bucket != null)
                garageRepository.deleteBucketById(bucket.id());
            postgresAdminRepository.dropDatabaseIfExist(req.name());

            e.printStackTrace();
            log.error(e.getMessage());
            throw new DatasetCreationException("Failed dataset creation", e);
        }
    }

    /*
     * Has to:
     * - Create a postgres database
     * - Create a garage bucket (if not local mode)
     * - Create a key with write access
     * - Upload given file
     * - Create ducklake table from given file
     * - Persist dataset and related info in backend postgres db
     */
    public Dataset createDatasetFromFile(CreateDatasetRequest req, FileUpload file) {
        if (!datasetRepository.findByName(req.name()).isEmpty()) {
            throw new DatasetAlreadyExistsException("Dataset with that name already exists", null);
        }

        String bucketName = "ducklake-cbh-" + UUID.randomUUID().toString();
        String keyName = "ducklake_cbh_" + UUID.randomUUID().toString();

        CreateBucketResponse bucket = null;
        CreateKeyResponse key = null;

        try {
            // Create postgres db
            postgresAdminRepository.createDatabase(req.name());

            if (localModeEnabled) {
                Path localFile = copyUploadToLocalDucklakePath(file, req.name());
                ducklakeRepository.createTable(new ConnectionArgs(
                        req.name(), "abc", "abc", "abc"), req.name(), localFile.toString());
            } else {
                // Create garage bucket and access key
                bucket = garageRepository.createBucket(bucketName);
                key = garageRepository.createKey(keyName, OffsetDateTime.now(), true);
                garageRepository.allowKey(key.accessKeyId(), bucket.id(), true);

                // Upload file to garage and get s3 path to said file
                String dataPath = uploadFileToGarage(file, bucketName);
                ducklakeRepository.createTable(new ConnectionArgs(
                        req.name(),
                        bucketName,
                        key.accessKeyId(),
                        key.secretAccessKey()), req.name(), dataPath);
            }

            // Persist dataset info in backend database
            Dataset d = new Dataset(
                    req.name(),
                    req.displayName(),
                    req.description(),
                    bucketName,
                    req.isPublic());

            return datasetRepository.save(d);
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());

            // Clean up bucket and database if dataset was unable to be created
            if (bucket != null)
                garageRepository.deleteBucketById(bucket.id());
            postgresAdminRepository.dropDatabaseIfExist(req.name());

            throw new DatasetCreationException("Failed dataset creation", e);
        } finally {
            // Remove temporary access key regardless if the dataset is created or not
            if (key != null)
                garageRepository.deleteKey(keyName);
        }
    }

    public void appendDataToDataset(String id, FileUpload file) {
        Dataset d = datasetRepository.findByIdOptional(UUID.fromString(id))
                .orElseThrow(() -> new NoSuchElementException("Dataset doesn't exist"));

        String keyName = "ducklake_cbh_" + UUID.randomUUID().toString();
        CreateKeyResponse key = null;

        try {
            if (localModeEnabled) {
                Path localFile = copyUploadToLocalDucklakePath(file, d.getName());
                ducklakeRepository.insertFile(new ConnectionArgs(
                        d.getName(),
                        "abc",
                        "abc",
                        "abc"), d.getName(), localFile.toString());
            } else {
                // Create temporary access key
                CreateBucketResponse bucket = garageRepository.getBucketByGlobalAlias(d.getBucketName());
                key = garageRepository.createKey(keyName, OffsetDateTime.now(), true);
                garageRepository.allowKey(key.accessKeyId(), bucket.id(), true);

                // Upload file to garage and get s3 path to said file
                String dataPath = uploadFileToGarage(file, d.getBucketName());
                ducklakeRepository.insertFile(new ConnectionArgs(
                        d.getName(),
                        d.getBucketName(),
                        key.accessKeyId(),
                        key.secretAccessKey()), d.getName(), dataPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            throw new DatasetInsertionException(
                    "Failed to insert '" + file.fileName() + "' into dataset '" + d.getName() + "'",
                    e);
        } finally {
            // Remove temporary access key regardless if the dataset is created or not
            if (key != null)
                garageRepository.deleteKey(keyName);
        }
    }

    public void updateDataset(String id, UpdateDatasetRequest req) {
        datasetRepository.update(id, req);
    }

    /*
     * Has to:
     * - Get dataset info
     * - Delete users associated with the db, through credentials table
     * - Delete database
     * - Delete garage keys associated with the bucket, through credentials table
     * - Delete bucket
     * - Delete dataset info
     */
    public void deleteDataset(String id) {
        try {
            // If no such dataset exists, no point in going further
            Dataset d = datasetRepository.findByIdOptional(UUID.fromString(id))
                    .orElseThrow(() -> new NoSuchElementException("Dataset doesn't exist"));

            // Retrieve user-created credentials so we can delete their
            // postgres users and garage access keys
            List<Credential> credentials = credentialRepository.listAllByDataset(d.getId());
            List<String> keys = credentials.stream().map(c -> c.getGarageAccessKeyId()).toList();
            List<String> users = credentials.stream().map(c -> c.getPostgresUsername()).toList();

            // Revoke access and drop users, then delete database
            postgresUserRepository.dropUsersFromDataset(d.getName(), users);
            postgresAdminRepository.dropDatabaseIfExist(d.getName());

            if (localModeEnabled) {
                FileUtils.deleteDirectory(new File(localDataPath + d.getName()));
                FileUtils.deleteDirectory(new File(localDataPath + "main/" + d.getName()));
            } else {
                // Delete all Garage Access Keys, then delete bucket
                for (String keyId : keys) {
                    garageRepository.deleteKey(keyId);
                }
                garageRepository.deleteBucketByGlobalAlias(d.getBucketName());
            }

            // Delete dataset info
            datasetRepository.deleteByIdSafe(d.getId());
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            throw new DatasetDeletionException("Failed dataset deletion", e);
        }
    }

    /*
     * HELPER METHODS
     */

    private String uploadFileToGarage(FileUpload file, String bucketName) throws IOException {
        // Establish path to file in garage for upload
        Path path = file.uploadedFile();
        String objectKey = "uploads/" + UUID.randomUUID() + "-" + file.fileName();

        try (InputStream stream = Files.newInputStream(path)) {
            garageRepository.upload(
                    objectKey,
                    stream,
                    Files.size(path),
                    file.contentType(),
                    bucketName);
        }

        // Establish path for ducklake and create table from said file
        return "s3://" + bucketName + "/" + objectKey;
    }

    // FOR LOCAL DEVELOPMENT: Saves uploaded file and retains extension and such.
    private Path copyUploadToLocalDucklakePath(FileUpload file, String datasetName) throws IOException {
        String originalFileName = Path.of(UUID.randomUUID().toString() + file.fileName()).getFileName().toString();

        // Optional but recommended: prevent weird filenames
        originalFileName = originalFileName.replaceAll("[^A-Za-z0-9._-]", "_");

        Path datasetDir = Path.of("ducklake_data", datasetName, "uploads");
        Files.createDirectories(datasetDir);

        Path target = datasetDir.resolve(originalFileName);

        Files.copy(
                file.uploadedFile(),
                target,
                StandardCopyOption.REPLACE_EXISTING);

        return target;
    }

}
