package com.se196693.mvc.repository;

import com.se196693.mvc.entity.UserAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAnswerRepository extends JpaRepository<UserAnswer, Long> {
    int countByExamAttemptIdAndIsCorrectTrue(Long attemptId);

    List<UserAnswer> findByExamAttemptId(Long attemptId);
    Optional<UserAnswer> findByExamAttemptIdAndQuestionId(Long attemptId, Long questionId);
}
