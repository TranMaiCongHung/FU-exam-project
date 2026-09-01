package com.se196693.mvc.specification;

import com.se196693.mvc.entity.User;
import com.se196693.mvc.enums.Role;
import com.se196693.mvc.enums.UserStatus;
import org.springframework.data.jpa.domain.Specification;

//loc du lieu
public class UserSpecification {
    public static Specification<User> hasKeyword(String keyword) {
        /*
        * root: đại diện cho bảng trong db
        * query: câu truy vấn tổng thể
        * criteriaBuilder: người xây dựng điều kiện*/
        return (root, query, criteriaBuilder) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }

            String pattern = "%" + keyword.toLowerCase() + "%";

            return criteriaBuilder.or(
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("username")),
                            pattern
                    ),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("fullName")),
                            pattern
                    ),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("email")),
                            pattern
                    )
            );
        };
    }

    public static Specification<User> hasRole(Role role) {
        return (root, query, criteriaBuilder) -> {

            if (role == null) {
                return null;
            }

            return criteriaBuilder.equal(
                    root.get("role"),
                    role
            );
        };
    }
    public static Specification<User> hasStatus(UserStatus status) {
        return (root, query, criteriaBuilder) -> {

            if (status == null) {
                return null;
            }

            return criteriaBuilder.equal(
                    root.get("status"),
                    status
            );
        };
    }

}
