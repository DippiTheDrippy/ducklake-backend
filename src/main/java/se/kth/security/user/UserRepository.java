package se.kth.security.user;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import se.kth.common.Pagination;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserRepository implements PanacheRepositoryBase<User, UUID> {

    public Optional<User> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }

    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }

    @Transactional
    public User save(User user) {
        persist(user);
        return user;
    }

    @Transactional
    public User upsertByEmail(String email, String firstName, String lastName) {
        Optional<User> existingUser = findByEmail(email);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            return user;
        }

        User user = new User(email, firstName, lastName);
        persist(user);
        return user;
    }

    @Transactional
    public boolean deleteByIdSafe(UUID id) {
        return deleteById(id);
    }

    public Pagination<User> listAll(int pageIndex, int pageSize) {
        PanacheQuery<User> query = findAll();
        long total = query.count();

        return new Pagination<>(query.page(pageIndex, pageSize).list(), pageIndex, pageSize, total);
    }

}
