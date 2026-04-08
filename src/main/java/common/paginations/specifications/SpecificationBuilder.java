package common.paginations.specifications;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.*;

@SuppressWarnings("unused")
public class SpecificationBuilder<T> {

    private final List<Specification<T>> specs = new ArrayList<>();

    public static <T> SpecificationBuilder<T> create() {
        return new SpecificationBuilder<>();
    }

    public SpecificationBuilder<T> and(Specification<T> spec) {
        if (spec != null) specs.add(spec);
        return this;
    }

    public SpecificationBuilder<T> keywordLike(String keyword, String... fields) {
        if (keyword == null || keyword.isBlank() || fields.length == 0) return this;
        String pattern = "%" + keyword.toLowerCase().trim() + "%";
        return and((root, q, cb) -> {
            Predicate[] predicates = Arrays.stream(fields)
                    .map(f -> cb.like(cb.lower(root.get(f).as(String.class)), pattern))
                    .toArray(Predicate[]::new);
            return cb.or(predicates);
        });
    }

    public SpecificationBuilder<T> eq(String field, Object value) {
        if (value == null) return this;
        return and((root, q, cb) -> cb.equal(root.get(field), value));
    }

    public SpecificationBuilder<T> notEq(String field, Object value) {
        if (value == null) return this;
        return and((root, q, cb) -> cb.notEqual(root.get(field), value));
    }

    public <E extends Enum<E>> SpecificationBuilder<T> enumEq(String field, E value) {
        if (value == null) return this;
        return and((root, q, cb) -> cb.equal(root.get(field), value));
    }

    public <E extends Enum<E>> SpecificationBuilder<T> enumIn(String field, Collection<E> values) {
        if (values == null || values.isEmpty()) return this;
        return and((root, q, cb) -> root.get(field).in(values));
    }

    public <E extends Enum<E>> SpecificationBuilder<T> enumEqRaw(
            String field, String raw, Class<E> enumClass) {
        if (raw == null || raw.isBlank()) return this;
        try {
            return enumEq(field, Enum.valueOf(enumClass, raw.toUpperCase().trim()));
        } catch (IllegalArgumentException e) {
            return this;
        }
    }

    public <E extends Enum<E>> SpecificationBuilder<T> enumInRaw(
            String field, String raw, Class<E> enumClass) {
        if (raw == null || raw.isBlank()) return this;
        List<E> values = Arrays.stream(raw.split(","))
                .map(s -> {
                    try {
                        return Enum.valueOf(enumClass, s.trim().toUpperCase());
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
        return enumIn(field, values);
    }

    public SpecificationBuilder<T> in(String field, Collection<?> values) {
        if (values == null || values.isEmpty()) return this;
        return and((root, q, cb) -> root.get(field).in(values));
    }

    public SpecificationBuilder<T> notIn(String field, Collection<?> values) {
        if (values == null || values.isEmpty()) return this;
        return and((root, q, cb) -> cb.not(root.get(field).in(values)));
    }

    public <Y extends Comparable<Y>> SpecificationBuilder<T> gt(String field, Y val) {
        if (val == null) return this;
        return and((root, q, cb) -> cb.greaterThan(root.get(field), val));
    }

    public <Y extends Comparable<Y>> SpecificationBuilder<T> gte(String field, Y val) {
        if (val == null) return this;
        return and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get(field), val));
    }

    public <Y extends Comparable<Y>> SpecificationBuilder<T> lt(String field, Y val) {
        if (val == null) return this;
        return and((root, q, cb) -> cb.lessThan(root.get(field), val));
    }

    public <Y extends Comparable<Y>> SpecificationBuilder<T> lte(String field, Y val) {
        if (val == null) return this;
        return and((root, q, cb) -> cb.lessThanOrEqualTo(root.get(field), val));
    }

    public SpecificationBuilder<T> between(String field, Instant from, Instant to) {
        gte(field, from);
        lte(field, to);
        return this;
    }

    public SpecificationBuilder<T> isNull(String field) {
        return and((root, q, cb) -> cb.isNull(root.get(field)));
    }

    public SpecificationBuilder<T> isNotNull(String field) {
        return and((root, q, cb) -> cb.isNotNull(root.get(field)));
    }

    public SpecificationBuilder<T> isTrue(String field) {
        return and((root, q, cb) -> cb.isTrue(root.get(field)));
    }

    public SpecificationBuilder<T> isFalse(String field) {
        return and((root, q, cb) -> cb.isFalse(root.get(field)));
    }

    public SpecificationBuilder<T> joinEq(String association, String field, Object value) {
        if (value == null) return this;
        return and((root, q, cb) -> cb.equal(root.join(association).get(field), value));
    }

    public SpecificationBuilder<T> andIf(boolean condition, Specification<T> spec) {
        return condition ? and(spec) : this;
    }

    public Specification<T> build() {
        if (specs.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return specs.stream().reduce(Specification::and).orElse((root, query, cb) -> cb.conjunction());
    }
}