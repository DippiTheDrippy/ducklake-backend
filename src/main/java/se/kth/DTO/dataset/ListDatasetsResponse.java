package se.kth.DTO.dataset;

import java.util.List;

import se.kth.common.Pagination;
import se.kth.model.Dataset;

public class ListDatasetsResponse extends Pagination<Dataset> {

    public ListDatasetsResponse(List<Dataset> items, int pageIndex, int pageSize, long totalItems) {
        super(items, pageIndex, pageSize, totalItems);
    }

}
