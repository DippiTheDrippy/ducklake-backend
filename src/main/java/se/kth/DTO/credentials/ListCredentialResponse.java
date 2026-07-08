package se.kth.DTO.credentials;

import java.util.List;

import se.kth.common.Pagination;
import se.kth.model.Credential;

public class ListCredentialResponse extends Pagination<Credential> {

    public ListCredentialResponse(List<Credential> items, int pageIndex, int pageSize, long totalItems) {
        super(items, pageIndex, pageSize, totalItems);
    }

}
