package common.paginations.converters;

import common.paginations.dtos.PageRequest;
import common.paginations.dtos.PageResponse;
import common.paginations.enums.SortDirection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.function.Function;

public final class SpringPageConverter {

    private SpringPageConverter() {}

    public static Pageable toPageable(PageRequest req) {
        Sort sort = Sort.by(
                req.validSorts().stream()
                        .map(sf -> sf.direction() == SortDirection.ASC
                                ? Sort.Order.asc(sf.field())
                                : Sort.Order.desc(sf.field()))
                        .toList()
        );
        return org.springframework.data.domain.PageRequest.of(
                req.validPage(), req.validSize(), sort);
    }

    public static <T> PageResponse<T> fromPage(Page<T> page) {
        return PageResponse.of(
                page.getContent(),
                page.hasNext(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber() + 1,
                page.getSize());
    }

    public static <T, R> PageResponse<R> fromPage(Page<T> page, Function<T, R> mapper) {
        return PageResponse.of(
                page.getContent().stream().map(mapper).toList(),
                page.hasNext(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber() + 1,
                page.getSize());
    }
}