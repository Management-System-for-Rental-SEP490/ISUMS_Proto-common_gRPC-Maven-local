package common.paginations.cache;

import common.paginations.dtos.PageRequest;
import common.paginations.dtos.SortField;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.TreeMap;

public final class CacheKeyBuilder {

    private static final int MAX_KEY_LENGTH = 200;

    private CacheKeyBuilder() {
    }

    public static String build(String namespace, PageRequest req) {
        String sortPart = req.validSorts().stream()
                .map(SortField::toCacheSegment)
                .reduce((a, b) -> a + "," + b)
                .orElse("default");

        StringBuilder sb = new StringBuilder()
                .append(namespace)
                .append(":p:").append(req.validPage())
                .append(":s:").append(req.validSize())
                .append(":o:").append(sortPart);

        if (req.hasKeyword()) {
            sb.append(":kw:").append(sanitize(req.keyword()));
        }

        if (req.filters() != null && !req.filters().isEmpty()) {
            String filterPart = new TreeMap<>(req.filters())
                    .entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .reduce((a, b) -> a + ";" + b)
                    .orElse("");
            sb.append(":f:").append(filterPart);
        }

        String key = sb.toString();
        return key.length() > MAX_KEY_LENGTH
                ? namespace + ":h:" + md5(key)
                : key;
    }

    public static String scanPattern(String namespace) {
        return namespace + ":*";
    }

    private static String sanitize(String s) {
        return s == null ? "" : s.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();
    }

    private static String md5(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("MD5")
                    .digest(input.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return String.valueOf(Math.abs(input.hashCode()));
        }
    }
}