package se.kth.credential;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import io.quarkus.security.UnauthorizedException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import se.kth.common.Pagination;
import se.kth.common.exceptions.PostgresAdminException;
import se.kth.common.exceptions.PostgresException;
import se.kth.dataset.Dataset;
import se.kth.dataset.DatasetRepository;
import se.kth.dataset.dto.CreateCredentialRequest;
import se.kth.dataset.dto.CreateCredentialResponse;
import se.kth.garage.GarageRepository;
import se.kth.garage.dto.CreateBucketResponse;
import se.kth.garage.dto.CreateKeyResponse;
import se.kth.postgres.PostgresUserRepository;
import se.kth.postgres.PostgresUserRepository.DbCredentials;
import se.kth.security.AccessLevel;
import se.kth.security.PermissionRepository;
import se.kth.security.user.User;
import se.kth.security.user.UserRepository;

@ApplicationScoped
public class CredentialService {

        @Inject
        UserRepository userRepository;

        @Inject
        CredentialRepository credentialRepository;

        @Inject
        DatasetRepository datasetRepository;

        @Inject
        PostgresUserRepository postgresUserRepository;

        @Inject
        GarageRepository garageRepository;

        @Inject
        PermissionRepository permissionRepository;

        public Credential getDatasetCredential(String datasetId, String email) {
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new NoSuchElementException("No such user!"));

                return credentialRepository.findByDatasetAndUser(UUID.fromString(datasetId), user.getId()).orElse(null);
        }

        public Pagination<Credential> listUserCredentials(String email, int pageIndex, int pageSize) {
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new NoSuchElementException("No such user!"));

                return credentialRepository.listByUser(user.getId(), pageIndex, pageSize);
        }

        /*
         * Create a set of credentials for a user for a dataset:
         * - Perform checks of whether the user has access to the existing dataset
         * - Create garage access key
         * - Create postgres user with access to the ducklake catalog database
         */
        public CreateCredentialResponse createCredential(String datasetId, String email, CreateCredentialRequest req) {
                // Locate user and dataset
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new BadRequestException("Could not locate user!"));
                Dataset d = datasetRepository.findByIdOptional(UUID.fromString(datasetId))
                                .orElseThrow(() -> new NoSuchElementException("Connected dataset could not be found!"));

                // Does user have request access level to the dataset?
                if (permissionRepository.hasAccessLevel(user.getId(), d.getId(), req.access())) {
                        // Create garage access key, access level is assigned below
                        String keyName = "ducklake_cbh_" + UUID.randomUUID().toString();
                        CreateBucketResponse bucket = garageRepository.getBucketByGlobalAlias(d.getBucketName());
                        CreateKeyResponse key = garageRepository.createKey(keyName, req.expiresAt(),
                                        req.neverExpires());
                        DbCredentials dbCred = null;

                        // Create READ or WRITE postgres user, assign READ or WRITE access to garage key
                        if (req.access().compareTo(AccessLevel.READ) == 0) {
                                dbCred = postgresUserRepository.createReadOnlyUser(d.getName(), req.expiresAt(),
                                                req.neverExpires());
                                garageRepository.allowKey(key.accessKeyId(), bucket.id(), false);
                        } else if (req.access().compareTo(AccessLevel.WRITE) == 0) {
                                dbCred = postgresUserRepository.createReadWriteUser(d.getName(), req.expiresAt(),
                                                req.neverExpires());
                                garageRepository.allowKey(key.accessKeyId(), bucket.id(), true);
                        }

                        if (dbCred == null) {
                                garageRepository.deleteKey(key.accessKeyId());
                                throw new PostgresException("Something went wrong! Unable to create postgres user");
                        }

                        Credential cred = new Credential(
                                        d.getId(),
                                        user.getId(),
                                        dbCred.username(),
                                        key.accessKeyId(),
                                        req.neverExpires() ? null : req.expiresAt());
                        credentialRepository.save(cred);

                        return new CreateCredentialResponse(
                                        cred.getId(),
                                        req.access(),
                                        d.getId(),
                                        user.getId(),
                                        d.getName(),
                                        d.getBucketName(),
                                        dbCred.username(),
                                        dbCred.password(),
                                        key.accessKeyId(),
                                        key.secretAccessKey(),
                                        req.neverExpires() ? null : req.expiresAt());
                } else {
                        throw new UnauthorizedException("User does not have corresponding access to this dataset!");
                }
        }

        /*
         * Rotate a user's dataset credentials.
         * - Generates a new password for the postgres user.
         * - Recreates the Garage Access Key.
         */
        public CreateCredentialResponse rotateCredential(String id, String email) {
                // Locate user, locate credential
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new BadRequestException("Could not locate user!"));
                Credential cred = credentialRepository.findByIdOptional(UUID.fromString(id))
                                .orElseThrow(() -> new NoSuchElementException("No such credential!"));

                if (cred.expired())
                        throw new BadRequestException("Credential has already expired.");

                // Make sure user owns the credential
                if (user.getId().compareTo(cred.getUserId()) != 0) {
                        throw new UnauthorizedException("User does not own this credential");
                }

                Dataset d = datasetRepository.findByIdOptional(cred.getDatasetId())
                                .orElseThrow(() -> new NoSuchElementException("Connected dataset could not be found!"));

                // Create new password for postgres user
                String password = postgresUserRepository.rotateUserPassword(cred.getPostgresUsername(),
                                cred.getExpiresAt(),
                                cred.getExpiresAt() == null);

                // Delete old access key
                garageRepository.deleteKey(cred.getGarageAccessKeyId());

                // Create garage access key, access level is assigned below
                String keyName = "ducklake_cbh_" + UUID.randomUUID().toString();
                CreateBucketResponse bucket = garageRepository.getBucketByGlobalAlias(d.getBucketName());
                CreateKeyResponse key = garageRepository.createKey(keyName, cred.getExpiresAt(),
                                cred.getExpiresAt() == null);
                garageRepository.allowKey(key.accessKeyId(), bucket.id(),
                                AccessLevel.valueOf(cred.getAccessLevel()).compareTo(AccessLevel.WRITE) == 0);

                credentialRepository.rotate(cred.getId(), key.accessKeyId());

                return new CreateCredentialResponse(
                                cred.getId(),
                                AccessLevel.valueOf(cred.getAccessLevel()),
                                d.getId(),
                                user.getId(),
                                d.getName(),
                                d.getBucketName(),
                                cred.getPostgresUsername(),
                                password,
                                key.accessKeyId(),
                                key.secretAccessKey(),
                                cred.getExpiresAt());
        }

        /*
         * Delete a user's dataset credentials.
         * - Checks whether the user owns the credential they are aiming to delete
         * - Delete postgres user, garage key, and credential entity in backend db
         */
        public void deleteCredential(String id, String email) {
                // Locate user, locate credential
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new BadRequestException("Could not locate user!"));

                Credential cred = credentialRepository.findByIdOptional(UUID.fromString(id))
                                .orElseThrow(() -> new NoSuchElementException("No such credential!"));

                // Make sure user owns the credential
                if (user.getId().compareTo(cred.getUserId()) != 0) {
                        throw new UnauthorizedException("User does not own this credential");
                }

                Dataset d = datasetRepository.findByIdOptional(cred.getDatasetId())
                                .orElseThrow(() -> new NoSuchElementException("Connected dataset could not be found!"));

                // delete postgres user, access key and then credential.
                postgresUserRepository.deleteUser(cred.getPostgresUsername(), d.getName());
                garageRepository.deleteKey(cred.getGarageAccessKeyId());
                credentialRepository.deleteByIdSafe(cred.getId());
        }

}
