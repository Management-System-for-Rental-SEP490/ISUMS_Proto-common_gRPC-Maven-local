package common.paginations.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PageResponse<T>(
        List<T> items,
        boolean hasMore,
        Long total,
        Integer totalPages,
        Integer currentPage
) {
    public static <T> PageResponse<T> of(
            List<T> items,
            boolean hasMore,
            long total,
            int totalPages,
            int currentPage) {
        return PageResponse.<T>builder()
                .items(items)
                .hasMore(hasMore)
                .total(total)
                .totalPages(totalPages)
                .currentPage(currentPage)
                .build();
    }

    public static <T> PageResponse<T> empty() {
        return PageResponse.<T>builder()
                .items(List.of())
                .hasMore(false)
                .total(0L)
                .totalPages(0)
                .currentPage(0)
                .build();
    }
}