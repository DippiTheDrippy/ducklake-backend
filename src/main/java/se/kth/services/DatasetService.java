package se.kth.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import se.kth.DTO.dataset.DatasetWithSummary;
import se.kth.DTO.garage.CreateBucketResponse;
import se.kth.DTO.garage.CreateKeyResponse;
import se.kth.common.Pagination;
import se.kth.common.exceptions.DatasetException;
import se.kth.model.Dataset;
import se.kth.model.Group;
import se.kth.model.JwtUser;
import se.kth.model.ducklake.TableSummary;
import se.kth.repositories.DatasetRepository;
import se.kth.repositories.DucklakeRepository;
import se.kth.repositories.GarageRepository;
import se.kth.repositories.KeycloakRepository;
import se.kth.repositories.PermissionRepository;
import se.kth.repositories.DucklakeRepository.ConnectionArgs;

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
    DucklakeRepository ducklakeRepository;

    @Inject
    GarageRepository garageRepository;

    @Inject
    KeycloakRepository keycloakRepository;

    @ConfigProperty(name = "app.ducklake.local.enabled", defaultValue = "false")
    boolean localModeEnabled;

    public Pagination<Dataset> listDatasets(int pageIndex, int pageSize) {
        return datasetRepository.listAll(pageIndex, pageSize);
    }

    public Pagination<Dataset> listUserDatasets(JwtUser user, int pageIndex, int pageSize) {
        List<UUID> groupIds = keycloakRepository.getAllGroupsForUser(user.id().toString())
                .stream()
                .map(Group::getId)
                .toList();

        return permissionRepository.findAccessibleDatasets(user.id(), groupIds, pageIndex, pageSize);
    }

    public DatasetWithSummary getDataset(String id) {
        Dataset d = datasetRepository.findById(UUID.fromString(id));
        if (d == null) {
            throw new NotFoundException("Dataset does not exist!");
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

    public DatasetWithSummary getDatasetForUser(String id, JwtUser user) {
        List<UUID> groupIds = keycloakRepository.getAllGroupsForUser(user.id().toString())
                .stream()
                .map(Group::getId)
                .toList();

        Dataset d = permissionRepository.findAccessibleDataset(user.id(), groupIds, UUID.fromString(id)).orElse(null);
        if (d == null) {
            throw new NotFoundException("Dataset does not exist!");
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
            throw new DatasetException("Failed to retrieve dataset summary", e);
        } finally {
            if (tempKey != null)
                garageRepository.deleteKey(tempKey.accessKeyId());
        }
        return resp;
    }

    public Pagination<Dataset> searchDatasets(String search, int pageIndex, int pageSize) {
        return datasetRepository.searchByNameOrDesc(search, pageIndex, pageSize);
    }

    public Pagination<Dataset> searchDatasetsForUser(String search, JwtUser user, int pageIndex, int pageSize) {
        List<UUID> groupIds = keycloakRepository.getAllGroupsForUser(user.id().toString())
                .stream()
                .map(Group::getId)
                .toList();

        return permissionRepository.findAccessibleDatasetsBySearch(user.id(), groupIds, search, pageIndex, pageSize);
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
