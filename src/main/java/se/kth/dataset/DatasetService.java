package se.kth.dataset;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import se.kth.common.Pagination;
import se.kth.security.KeycloakUser;
import se.kth.security.PermissionRepository;
import se.kth.security.user.User;
import se.kth.security.user.UserRepository;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class DatasetService {

    @Inject
    DatasetRepository datasetRepository;

    @Inject
    PermissionRepository permissionRepository;

    @Inject
    UserRepository userRepository;

    public Pagination<Dataset> listDatasets(int pageIndex, int pageSize) {
        return datasetRepository.listAll(pageIndex, pageSize);
    }

    public Pagination<Dataset> listUserDatasets(KeycloakUser kUser, int pageIndex, int pageSize) {
        User user = userRepository.findByEmail(kUser.email())
                .orElseThrow(() -> new IllegalArgumentException("User does not exist: " + kUser.email()));

        // Update with query params
        return permissionRepository.findAccessibleDatasets(user.getId(), pageIndex, pageSize);
    }

    public Dataset getDataset(String id) {
        return datasetRepository.findById(UUID.fromString(id));
    }

    public Dataset getDatasetForUser(String id, KeycloakUser kUser) {
        User user = userRepository.findByEmail(kUser.email())
                .orElseThrow(() -> new IllegalArgumentException("User does not exist: " + kUser.email()));

        return permissionRepository.findAccessibleDataset(user.getId(), UUID.fromString(id)).orElse(null);
    }

    public Pagination<Dataset> searchDatasets(String search, int pageIndex, int pageSize) {
        return datasetRepository.searchByNameOrDesc(search, pageIndex, pageSize);
    }

    public Pagination<Dataset> searchDatasetsForUser(String search, KeycloakUser kUser, int pageIndex, int pageSize) {
        User user = userRepository.findByEmail(kUser.email())
                .orElseThrow(() -> new IllegalArgumentException("User does not exist: " + kUser.email()));

        return permissionRepository.findAccessibleDatasetsBySearch(user.getId(), search, pageIndex, pageSize);
    }
}
