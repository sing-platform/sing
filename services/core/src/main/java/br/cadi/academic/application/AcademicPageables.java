package br.cadi.academic.application;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class AcademicPageables {

    public static final int MAX_PAGE_SIZE = 100;

    /**
     * Campos válidos para ORDER BY em consultas {@code SchoolRoom} (evita 500 com {@code sort=string} do Swagger).
     */
    private static final Set<String> SCHOOL_ROOM_SORT_FIELDS = Set.of(
            "name", "id", "isActive", "classroom", "capacity"
    );

    /**
     * Campos válidos para ORDER BY em consultas {@code Enrollment}.
     */
    private static final Set<String> ENROLLMENT_SORT_FIELDS = Set.of(
            "enrollmentDate", "id", "status"
    );

    private AcademicPageables() {
    }

    public static Pageable normalize(Pageable pageable, Sort defaultSort) {
        int size = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : defaultSort;
        return PageRequest.of(pageable.getPageNumber(), size, sort);
    }

    public static Pageable normalizeSchoolRoomPageable(Pageable pageable) {
        return normalizeWithWhitelist(pageable, defaultSchoolRoomSort(), SCHOOL_ROOM_SORT_FIELDS);
    }

    public static Pageable normalizeEnrollmentPageable(Pageable pageable) {
        return normalizeWithWhitelist(pageable, defaultEnrollmentSort(), ENROLLMENT_SORT_FIELDS);
    }

    private static Pageable normalizeWithWhitelist(Pageable pageable, Sort defaultSort, Set<String> allowedProperties) {
        int size = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);
        List<Sort.Order> kept = new ArrayList<>();
        for (Sort.Order o : pageable.getSort()) {
            if (allowedProperties.contains(o.getProperty())) {
                kept.add(o);
            }
        }
        Sort sort = kept.isEmpty() ? defaultSort : Sort.by(kept);
        return PageRequest.of(pageable.getPageNumber(), size, sort);
    }

    public static Sort defaultEnrollmentSort() {
        return Sort.by(Sort.Direction.DESC, "enrollmentDate")
                .and(Sort.by(Sort.Direction.DESC, "id"));
    }

    public static Sort defaultSchoolRoomSort() {
        return Sort.by(Sort.Direction.ASC, "name");
    }
}
