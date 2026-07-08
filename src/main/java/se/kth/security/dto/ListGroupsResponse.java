package se.kth.security.dto;

import java.util.List;

import io.vertx.mutiny.ext.auth.User;
import se.kth.common.Pagination;
import se.kth.security.keycloak.Group;

public class ListGroupsResponse extends Pagination<Group> {

    public ListGroupsResponse(List<Group> items, int pageIndex, int pageSize, long totalItems) {
        super(items, pageIndex, pageSize, totalItems);
    }

}
