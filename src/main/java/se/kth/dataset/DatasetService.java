package se.kth.dataset;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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

    public List<Dataset> listDatasets() {
        return datasetRepository.listAll();
    }

    public List<Dataset> listUserDatasets(KeycloakUser kUser) {
        User user = userRepository.findByEmail(kUser.email())
                .orElseThrow(() -> new IllegalArgumentException("User does not exist: " + kUser.email()));

        // Update with query params
        return permissionRepository.findAccessibleDatasets(user.getId(), 1000, 0);
    }

    public Dataset getDataset(String id) {
        return datasetRepository.findById(UUID.fromString(id));
    }


    public Dataset getDatasetForUser(String id, KeycloakUser kUser) {
        User user = userRepository.findByEmail(kUser.email())
                .orElseThrow(() -> new IllegalArgumentException("User does not exist: " + kUser.email()));

        return permissionRepository.findAccessibleDataset(user.getId(), UUID.fromString(id)).orElse(null);
    }
}
