package se.kth.dataset;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

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
    public Dataset upsertByName(String name) {
        Optional<Dataset> existingDataset = findByName(name);

        if (existingDataset.isPresent()) {
            Dataset dataset = existingDataset.get();
            // set new stuffs
            return dataset;
        }

        // set fields in constructor
        Dataset dataset = new Dataset();
        persist(dataset);
        return dataset;
    }

    @Transactional
    public boolean deleteByIdSafe(UUID id) {
        return deleteById(id);
    }

}
