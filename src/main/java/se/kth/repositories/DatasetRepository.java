package se.kth.repositories;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import se.kth.DTO.dataset.UpdateDatasetRequest;
import se.kth.common.Pagination;
import se.kth.model.Dataset;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class DatasetRepository implements PanacheRepositoryBase<Dataset, UUID> {

    public Pagination<Dataset> listAll(int pageIndex, int pageSize) {
        PanacheQuery<Dataset> query = findAll();

        long total = query.count();

        return new Pagination<>(
                query.page(pageIndex, pageSize).list(),
                pageIndex,
                pageSize,
                total);
    }

    public Optional<Dataset> findByName(String name) {
        return find("name", name).firstResultOptional();
    }

    public Pagination<Dataset> searchByNameOrDesc(String search, int pageIndex, int pageSize) {
        String pattern = "%" + search.toLowerCase() + "%";

        PanacheQuery<Dataset> query = find("""
                lower(name) like ?1 or
                lower(description) like ?1 or
                lower(displayName) like ?1
                """, pattern);
        long total = query.count();

        return new Pagination<>(
                query.page(pageIndex, pageSize).list(),
                pageIndex,
                pageSize,
                total);
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

        dataset.setDisplayName(req.displayName());
        dataset.setDescription(req.description());
        dataset.setPublic(req.isPublic());

        return dataset;
    }

    @Transactional
    public boolean deleteByIdSafe(UUID id) {
        return deleteById(id);
    }

}
