package se.kth.favorite;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import se.kth.common.Pagination;
import se.kth.dataset.Dataset;
import se.kth.security.user.User;
import se.kth.security.user.UserRepository;

@ApplicationScoped
public class FavoriteService {

    @Inject
    UserRepository userRepository;

    @Inject
    FavoriteRepository favoriteRepository;

    public Pagination<Dataset> listFavoritedDatasets(String email, int pageIndex, int pageSize) {
        User user = getUser(email);
        return favoriteRepository.listFavortiedDatasets(user.getId(), pageIndex, pageSize);
    }

    public void addFavorite(String id, String email) {
        User user = getUser(email);
        favoriteRepository.addFavorite(user.getId(), UUID.fromString(id));
    }

    public void removeFavorite(String id, String email) {
        User user = getUser(email);
        favoriteRepository.removeFavorite(user.getId(), UUID.fromString(id));
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User does not exist!"));
    }

}
