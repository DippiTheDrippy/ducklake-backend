package se.kth.dataset.dto;

import java.util.List;

import se.kth.common.Pagination;
import se.kth.credential.Credential;

public class ListCredentialResponse extends Pagination<Credential> {

    public ListCredentialResponse(List<Credential> items, int pageIndex, int pageSize, long totalItems) {
        super(items, pageIndex, pageSize, totalItems);
    }

}
