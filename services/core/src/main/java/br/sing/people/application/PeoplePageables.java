package br.sing.people.application;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class PeoplePageables {

    public static final int MAX_PAGE_SIZE = 100;

    private static final Set<String> STUDENT_SORT_FIELDS = Set.of(
            "name", "id", "email", "grade", "birthDate"
    );

    private PeoplePageables() {
    }

    public static Pageable normalizeStudentPageable(Pageable pageable) {
        int size = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);
        List<Sort.Order> kept = new ArrayList<>();
        for (Sort.Order o : pageable.getSort()) {
            if (STUDENT_SORT_FIELDS.contains(o.getProperty())) {
                kept.add(o);
            }
        }
        Sort sort = kept.isEmpty() ? Sort.by(Sort.Direction.ASC, "name") : Sort.by(kept);
        return PageRequest.of(pageable.getPageNumber(), size, sort);
    }
}
