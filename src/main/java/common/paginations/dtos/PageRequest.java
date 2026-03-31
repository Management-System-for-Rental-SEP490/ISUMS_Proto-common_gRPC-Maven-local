package common.paginations.dtos;

import lombok.Builder;
import lombok.With;

@Builder
@With
public record PageRequest(
        int page,
        int size,
        String sortBy,
        String sortDir
) {
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public static PageRequest of(int page, int size) {
        return PageRequest.builder()
                .page(Math.max(page, 0))
                .size(Math.min(size, MAX_SIZE))
                .sortBy("createdAt")
                .sortDir("DESC")
                .build();
    }

    public int validSize() {
        return size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
    }
}
