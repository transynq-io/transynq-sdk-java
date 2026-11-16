# Pagination and grid queries

## Paged collections

Collections answer `PagedResponse { items, page, pageSize, total }` (`page` starts at 1, `pageSize` 1..200, default 50;
`sort` is a comma list such as `-date,name` from the resource's whitelist). Operations that return one also have an
**auto-paging** variant that reads page after page until an empty page or `page * pageSize >= total`.

```java
// one page
PagedResponse<PublicHolidayResponse> page = client.referenceData().listPublicHolidays(
        ListPublicHolidaysQuery.builder().countryId(1).year(2026).page(1).pageSize(50).sort("-date").build()).data();
System.out.println(page.items().size() + " of " + page.total());

// every item, page by page (a lazy Iterable; Paging.stream(...) gives a Stream)
for (PublicHolidayResponse h : client.referenceData().listPublicHolidaysAutoPaging(
        ListPublicHolidaysQuery.builder().countryId(1).year(2026).build(), null)) {
    System.out.println(h.date() + ": " + h.name());
}
```

## Grid queries (large lists)

Large lists - jobs, invoices, manifests - are read through the grid endpoints: `GET /api/v1/grids` lists the grids,
`GET /api/v1/grids/{id}` is a grid's definition (its columns, filter operators and sort keys are the whitelist), and
`POST /api/v1/grids/{id}/query` reads one page of rows (page size up to 500, at most 4 sort keys; filter values as text, dates
`yyyy-MM-dd` or relative such as `today-30`). The SDK's grid helper iterates every row:

```java
GridDefinitionResponse definition = client.grids().getGrid("jobs").data();   // the columns and operators you may use
GridQueryRequest request = GridQueryRequest.builder()
        .columns(List.of("jobNumber", "customer", "status"))
        .filters(List.of(GridFilterDto.builder().column("status").operator("eq").values(List.of("Booked")).build()))
        .pageSize(500)
        .build();
for (ObjectNode row : client.gridRows("jobs", request, null)) {
    System.out.println(row.get("jobNumber"));
}
```

Value lists (`.../facets/{column}`), CSV export (`.../export`, up to 100,000 rows, a streamed file) and saved views
(`.../views/{viewId}`) are ordinary operations of the Grids group; see the [resource index](resources/README.md#grids).
