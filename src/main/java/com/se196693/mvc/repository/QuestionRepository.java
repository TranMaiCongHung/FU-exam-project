package com.se196693.mvc.repository;

import com.se196693.mvc.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByExamIdOrderByQuestionNumberAsc(Long examId);

    boolean existsByExamIdAndQuestionNumber(Long examId, Integer questionNumber);

    @Query("select coalesce(max(q.questionNumber), 0) from Question q where q.exam.id = :examId")
    Integer findMaxQuestionNumberByExamId(@Param("examId") Long examId);

    @Query("select q.questionNumber from Question q where q.exam.id = :examId and q.questionNumber in :numbers")
    List<Integer> findExistingQuestionNumbers(@Param("examId") Long examId, @Param("numbers") Collection<Integer> numbers);
}
