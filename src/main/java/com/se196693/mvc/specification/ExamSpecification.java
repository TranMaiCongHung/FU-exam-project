package com.se196693.mvc.specification;

import com.se196693.mvc.entity.Exam;
import com.se196693.mvc.enums.ExamType;
import org.springframework.data.jpa.domain.Specification;

public class ExamSpecification {
    public static Specification<Exam> hasSubject(Long subjectId) {
        return (root, query, criteriaBuilder) -> {
            if (subjectId == null) {
                return null;
            }
            return criteriaBuilder.equal(
                    root.get("subject").get("id"),
                    subjectId
            );
        };
    }

    public static Specification<Exam> hasType(ExamType examType) {
        return (root, query, criteriaBuilder) -> {
            if (examType == null) {
                return null;
            }
            return criteriaBuilder.equal(root.get("examType"), examType);
        };
    }
}
