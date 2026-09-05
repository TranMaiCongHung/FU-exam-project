package com.se196693.mvc.repository;

import com.se196693.mvc.dto.response.SubjectResponse;
import com.se196693.mvc.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    List<Subject> findSubjectsByTermNumber(Integer termNumber);
}
