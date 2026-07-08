package se.kth.DTO.user;

import java.util.List;

import io.vertx.mutiny.ext.auth.User;
import se.kth.common.Pagination;

public class ListUsersResponse extends Pagination<User> {

    public ListUsersResponse(List<User> items, int pageIndex, int pageSize, long totalItems) {
        super(items, pageIndex, pageSize, totalItems);
    }

}
