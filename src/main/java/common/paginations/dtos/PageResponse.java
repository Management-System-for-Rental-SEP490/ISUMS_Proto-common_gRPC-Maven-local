package common.paginations.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PageResponse<T>(
        List<T> items,
        boolean hasMore,
        long total,
        int totalPages,
        int currentPage,
        int pageSize
) {
    public static <T> PageResponse<T> of(
            List<T> items, boolean hasMore,
            long total, int totalPages, int currentPage, int pageSize) {
        return PageResponse.<T>builder()
                .items(items).hasMore(hasMore).total(total)
                .totalPages(totalPages).currentPage(currentPage).pageSize(pageSize)
                .build();
    }

    public static <T> PageResponse<T> empty() {
        return PageResponse.<T>builder()
                .items(List.of()).hasMore(false).total(0L)
                .totalPages(0).currentPage(0).pageSize(PageRequest.DEFAULT_SIZE)
                .build();
    }

    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }

    public <R> PageResponse<R> map(java.util.function.Function<T, R> mapper) {
        return PageResponse.of(
                items.stream().map(mapper).toList(),
                hasMore, total, totalPages, currentPage, pageSize);
    }
}