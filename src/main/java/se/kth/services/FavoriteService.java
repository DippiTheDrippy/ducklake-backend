package se.kth.services;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import se.kth.common.Pagination;
import se.kth.model.Dataset;
import se.kth.model.JwtUser;
import se.kth.repositories.FavoriteRepository;

@ApplicationScoped
public class FavoriteService {

    @Inject
    FavoriteRepository favoriteRepository;

    public Pagination<Dataset> listFavoritedDatasets(JwtUser user, int pageIndex, int pageSize) {
        return favoriteRepository.listFavortiedDatasets(user.id(), pageIndex, pageSize);
    }

    public void addFavorite(JwtUser user, String id) {
        favoriteRepository.addFavorite(user.id(), UUID.fromString(id));
    }

    public void removeFavorite(JwtUser user, String id) {
        favoriteRepository.removeFavorite(user.id(), UUID.fromString(id));
    }

}
