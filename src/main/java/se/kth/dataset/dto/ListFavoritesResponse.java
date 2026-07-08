package se.kth.dataset.dto;

import java.util.List;

import se.kth.common.Pagination;
import se.kth.favorite.Favorite;

public class ListFavoritesResponse extends Pagination<Favorite> {

    public ListFavoritesResponse(List<Favorite> items, int pageIndex, int pageSize, long totalItems) {
        super(items, pageIndex, pageSize, totalItems);
    }

}
