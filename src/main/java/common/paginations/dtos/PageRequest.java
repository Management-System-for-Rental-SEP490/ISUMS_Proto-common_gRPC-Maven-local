package common.paginations.dtos;

import lombok.Builder;
import lombok.With;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Builder
@With
@SuppressWarnings("unused")
public record PageRequest(
        int page,
        int size,
        String keyword,
        List<SortField> sorts,
        Map<String, Object> filters
) {
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private static final List<SortField> DEFAULT_SORTS =
            List.of(SortField.desc("createdAt"));

    public static PageRequest defaults() {
        return PageRequest.builder()
                .page(DEFAULT_PAGE)
                .size(DEFAULT_SIZE)
                .sorts(DEFAULT_SORTS)
                .filters(Map.of())
                .build();
    }

    public int validPage() {
        return Math.max(page, 0);
    }

    public int validSize() {
        return size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
    }

    public List<SortField> validSorts() {
        return (sorts == null || sorts.isEmpty()) ? DEFAULT_SORTS : sorts;
    }

    public boolean hasKeyword() {
        return keyword != null && !keyword.isBlank();
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> filterValue(String key) {
        if (filters == null) return Optional.empty();
        return Optional.ofNullable((T) filters.get(key));
    }
}