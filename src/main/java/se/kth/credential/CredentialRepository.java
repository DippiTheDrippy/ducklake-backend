package se.kth.credential;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CredentialRepository implements PanacheRepositoryBase<Credential, UUID> {

    public Optional<Credential> findByDataset(UUID datasetId) {
        return find("datasetId", datasetId).firstResultOptional();
    }

    public List<Credential> listByDataset(UUID datasetId) {
        return list("datasetId", datasetId);
    }

    public List<Credential> listByUser(UUID userId) {
        return list("userId", userId);
    }

    public boolean existsByEmail(UUID datasetId) {
        return findByDataset(datasetId).isPresent();
    }

    @Transactional
    public Credential save(Credential cred) {
        persist(cred);
        return cred;
    }

    @Transactional
    public Credential rotate(String id) {
        Credential cred = findByIdOptional(UUID.fromString(id))
                .orElseThrow(() -> new NoSuchElementException("No such credential exists!"));
        // Rotate secrets
        return cred;

    }

    @Transactional
    public boolean deleteByIdSafe(UUID id) {
        return deleteById(id);
    }

}
