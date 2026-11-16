package com.transynq.sdk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * A page of a collection ({@code GET /resource?page=&pageSize=}).
 *
 * @param items the items of this page
 * @param page the 1-based page number
 * @param pageSize the page size
 * @param total the total number of items
 * @param <T> the item type
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PagedResponse<T>(@JsonProperty("items") List<T> items, @JsonProperty("page") int page, @JsonProperty("pageSize") int pageSize,
        @JsonProperty("total") long total) {
    /** Never null items. */
    public PagedResponse {
        items = items == null ? List.of() : java.util.Collections.unmodifiableList(new java.util.ArrayList<>(items));
    }
}
