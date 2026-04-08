package common.paginations.dtos;

import common.paginations.enums.SortDirection;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@SuppressWarnings("unused")
public class PageRequestParams {

    @Min(1)
    private int page = 1;

    @Min(1)
    @Max(100)
    private int size = PageRequest.DEFAULT_SIZE;

    private String keyword;

    private String sorts;

    private String sortBy;
    private String sortDir;

    private String status;
    private String statuses;
    private String houseId;

    private Map<String, String> filter = new HashMap<>();

    public PageRequest toPageRequest() {
        Map<String, Object> filters = new HashMap<>(filter);
        if (status   != null) filters.put("status",   status);
        if (statuses != null) filters.put("statuses", statuses);
        if (houseId  != null) filters.put("houseId",  houseId);
        return PageRequest.builder()
                .page(page - 1)
                .size(size)
                .keyword(keyword != null ? keyword.trim() : null)
                .sorts(parseSorts())
                .filters(filters)
                .build();
    }

    private List<SortField> parseSorts() {
        List<SortField> result = new ArrayList<>();

        if (sorts != null && !sorts.isBlank()) {
            for (String token : sorts.split(",")) {
                String[] parts = token.trim().split(":");
                if (parts.length >= 1 && !parts[0].isBlank()) {
                    SortDirection dir = parts.length >= 2
                            ? SortDirection.of(parts[1])
                            : SortDirection.DESC;
                    result.add(SortField.of(parts[0].trim(), dir));
                }
            }
        }

        if (result.isEmpty() && sortBy != null && !sortBy.isBlank()) {
            result.add(SortField.of(sortBy.trim(), SortDirection.of(sortDir)));
        }

        return result;
    }
}