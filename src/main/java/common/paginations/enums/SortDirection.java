package common.paginations.enums;

public enum SortDirection {
    ASC, DESC;

    public static SortDirection of(String value) {
        if (value == null || value.isBlank()) return DESC;
        try {
            return valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return DESC;
        }
    }
}