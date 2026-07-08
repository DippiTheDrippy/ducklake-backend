package se.kth.repositories;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import se.kth.common.Pagination;
import se.kth.model.Credential;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CredentialRepository implements PanacheRepositoryBase<Credential, UUID> {

    public Optional<Credential> findByDatasetAndUser(UUID datasetId, UUID userId) {
        return find("datasetId = ?1 and userId = ?2", datasetId, userId).firstResultOptional();
    }

    public Pagination<Credential> listByUser(UUID userId, int pageIndex, int pageSize) {
        PanacheQuery<Credential> query = find("userId = ?1", userId);
        long total = query.count();

        return new Pagination<>(
                query.page(pageIndex, pageSize).list(),
                pageIndex,
                pageSize,
                total);
    }

    public List<Credential> listAllByDataset(UUID datasetId) {
        return list("datasetId", datasetId);
    }

    @Transactional
    public Credential save(Credential cred) {
        persist(cred);
        return cred;
    }

    @Transactional
    public Credential rotate(UUID id, String accessKeyId) {
        Credential cred = findByIdOptional(id)
                .orElseThrow(() -> new NoSuchElementException("No such credential exists!"));

        cred.setGarageAccessKeyId(accessKeyId);

        return cred;

    }

    @Transactional
    public boolean deleteByIdSafe(UUID id) {
        return deleteById(id);
    }

}
