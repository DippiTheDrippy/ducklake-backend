package se.kth.common;

import java.util.List;

import lombok.Data;

@Data
public class Pagination<T> {

    private List<T> items;
    private int pageIndex;
    private int pageSize;
    private long totalItems;
    private int totalPages;

    public Pagination(List<T> items, int pageIndex, int pageSize, long totalItems) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be greater than 0");
        }

        this.items = items;
        this.pageIndex = pageIndex;
        this.pageSize = pageSize;
        this.totalItems = totalItems;
        // Calculate totalPages: Integer division will always round down, so we add
        // pageSize - 1 to totalItems to ensure correct number of total pages
        this.totalPages = (int) ((totalItems + pageSize - 1) / pageSize);
    }

}
