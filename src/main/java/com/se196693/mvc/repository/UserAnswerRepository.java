package com.se196693.mvc.repository;

import com.se196693.mvc.entity.UserAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAnswerRepository extends JpaRepository<UserAnswer, Long> {
    int countByExamAttemptIdAndIsCorrectTrue(Long attemptId);
}
