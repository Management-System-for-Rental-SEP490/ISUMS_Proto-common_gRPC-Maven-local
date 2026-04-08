package common.paginations.dtos;

import common.paginations.enums.SortDirection;

@SuppressWarnings("unused")
public record SortField(String field, SortDirection direction) {

    public static SortField of(String field, SortDirection direction) {
        return new SortField(field, direction);
    }

    public static SortField desc(String field) {
        return new SortField(field, SortDirection.DESC);
    }

    public static SortField asc(String field) {
        return new SortField(field, SortDirection.ASC);
    }

    public String toCacheSegment() {
        return field + "." + direction.name().charAt(0);
    }
}