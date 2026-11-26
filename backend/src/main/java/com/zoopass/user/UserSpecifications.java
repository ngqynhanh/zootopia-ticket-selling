package com.zoopass.user;

import static com.zoopass.common.web.SpecificationUtils.containsPattern;
import static com.zoopass.common.web.SpecificationUtils.hasText;

import com.zoopass.user.dto.UserRequests;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** Builds the WHERE clause of the admin user search from the optional filters. */
final class UserSpecifications {

    private UserSpecifications() {
    }

    static Specification<User> matching(UserRequests.Search search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (hasText(search.q())) {
                String pattern = containsPattern(search.q());
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), pattern, '\\'),
                        cb.like(cb.lower(root.get("email")), pattern, '\\'),
                        cb.like(cb.lower(cb.coalesce(root.get("phone"), "")), pattern, '\\')));
            }
            if (search.role() != null) {
                predicates.add(cb.equal(root.get("role"), search.role()));
            }
            if (search.active() != null) {
                predicates.add(cb.equal(root.get("active"), search.active()));
            }
            if (search.emailVerified() != null) {
                predicates.add(cb.equal(root.get("emailVerified"), search.emailVerified()));
            }
            if (search.createdFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), search.createdFrom()));
            }
            if (search.createdTo() != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), search.createdTo()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
