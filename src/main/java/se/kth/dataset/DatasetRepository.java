package se.kth.dataset;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import se.kth.admin.dto.UpdateDatasetRequest;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class DatasetRepository implements PanacheRepositoryBase<Dataset, UUID> {

    public Optional<Dataset> findByName(String name) {
        return find("name", name).firstResultOptional();
    }

    @Transactional
    public Dataset save(Dataset dataset) {
        persist(dataset);
        return dataset;
    }

    @Transactional
    public Dataset update(String id,
            UpdateDatasetRequest req) {
        Dataset dataset = findByIdOptional(UUID.fromString(id))
                .orElseThrow(() -> new NoSuchElementException("Dataset does not exist!"));

        dataset.setDisplayName(req.display_name());
        dataset.setDescription(req.description());
        dataset.setPublic(req.is_public());

        return dataset;
    }

    @Transactional
    public boolean deleteByIdSafe(UUID id) {
        return deleteById(id);
    }

}
