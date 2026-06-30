package se.kth.dataset;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import se.kth.common.Pagination;
import se.kth.common.exceptions.DatasetException;
import se.kth.dataset.dto.DatasetWithSummary;
import se.kth.ducklake.DucklakeRepository;
import se.kth.ducklake.DucklakeRepository.ConnectionArgs;
import se.kth.ducklake.model.TableSummary;
import se.kth.garage.GarageRepository;
import se.kth.garage.dto.CreateBucketResponse;
import se.kth.garage.dto.CreateKeyResponse;
import se.kth.security.KeycloakUser;
import se.kth.security.PermissionRepository;
import se.kth.security.user.User;
import se.kth.security.user.UserRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Slf4j
@ApplicationScoped
public class DatasetService {

    @Inject
    DatasetRepository datasetRepository;

    @Inject
    PermissionRepository permissionRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    DucklakeRepository ducklakeRepository;

    @Inject
    GarageRepository garageRepository;

    @ConfigProperty(name = "app.ducklake.local.enabled", defaultValue = "false")
    boolean localModeEnabled;

    public Pagination<Dataset> listDatasets(int pageIndex, int pageSize) {
        return datasetRepository.listAll(pageIndex, pageSize);
    }

    public Pagination<Dataset> listUserDatasets(KeycloakUser kUser, int pageIndex, int pageSize) {
        User user = userRepository.findByEmail(kUser.email())
                .orElseThrow(() -> new IllegalArgumentException("User does not exist: " + kUser.email()));

        // Update with query params
        return permissionRepository.findAccessibleDatasets(user.getId(), pageIndex, pageSize);
    }

    public DatasetWithSummary getDataset(String id) {
        Dataset d = datasetRepository.findById(UUID.fromString(id));
        if (d == null) {
            throw new BadRequestException("Could not find dataset!");
        }

        CreateKeyResponse tempKey = null;
        DatasetWithSummary resp = null;

        try {
            if (localModeEnabled) {
                List<TableSummary> summary = ducklakeRepository.summary(new ConnectionArgs(
                        d.getName(),
                        d.getBucketName(),
                        "abc",
                        "abc"), d.getName());

                resp = new DatasetWithSummary(d, summary);
            } else {
                tempKey = createTempKey(d.getBucketName());
                List<TableSummary> summary = ducklakeRepository.summary(new ConnectionArgs(
                        d.getName(),
                        d.getBucketName(),
                        tempKey.accessKeyId(),
                        tempKey.secretAccessKey()), d.getName());

                resp = new DatasetWithSummary(d, summary);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new DatasetException("Failed to retrieve dataset summary");
        } finally {
            if (tempKey != null)
                garageRepository.deleteKey(tempKey.accessKeyId());
        }

        return resp;
    }

    public DatasetWithSummary getDatasetForUser(String id, KeycloakUser kUser) {
        User user = userRepository.findByEmail(kUser.email())
                .orElseThrow(() -> new IllegalArgumentException("User does not exist: " + kUser.email()));

        Dataset d = permissionRepository.findAccessibleDataset(user.getId(), UUID.fromString(id)).orElse(null);
        if (d == null) {
            throw new BadRequestException("Could not find dataset!");
        }

        CreateKeyResponse tempKey = null;
        DatasetWithSummary resp = null;

        try {
            if (localModeEnabled) {
                List<TableSummary> summary = ducklakeRepository.summary(new ConnectionArgs(
                        d.getName(),
                        d.getBucketName(),
                        "abc",
                        "abc"), d.getName());

                resp = new DatasetWithSummary(d, summary);
            } else {
                tempKey = createTempKey(d.getBucketName());
                List<TableSummary> summary = ducklakeRepository.summary(new ConnectionArgs(
                        d.getName(),
                        d.getBucketName(),
                        tempKey.accessKeyId(),
                        tempKey.secretAccessKey()), d.getName());

                resp = new DatasetWithSummary(d, summary);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new DatasetException("Failed to retrieve dataset summary");
        } finally {
            if (tempKey != null)
                garageRepository.deleteKey(tempKey.accessKeyId());
        }
        return resp;
    }

    public Pagination<Dataset> searchDatasets(String search, int pageIndex, int pageSize) {
        return datasetRepository.searchByNameOrDesc(search, pageIndex, pageSize);
    }

    public Pagination<Dataset> searchDatasetsForUser(String search, KeycloakUser kUser, int pageIndex, int pageSize) {
        User user = userRepository.findByEmail(kUser.email())
                .orElseThrow(() -> new IllegalArgumentException("User does not exist: " + kUser.email()));

        return permissionRepository.findAccessibleDatasetsBySearch(user.getId(), search, pageIndex, pageSize);
    }

    private CreateKeyResponse createTempKey(String bucketName) {
        String keyName = "ducklake_cbh_" + UUID.randomUUID().toString();
        CreateKeyResponse key = null;

        try {
            CreateBucketResponse bucket = garageRepository.getBucketByGlobalAlias(bucketName);
            key = garageRepository.createKey(keyName, OffsetDateTime.now(), true);
            garageRepository.allowKey(key.accessKeyId(), bucket.id(), true);
        } catch (Exception e) {
            if (key != null)
                garageRepository.deleteKey(key.accessKeyId());
            log.error(e.getMessage());
            throw e;
        }

        return key;
    }
}
