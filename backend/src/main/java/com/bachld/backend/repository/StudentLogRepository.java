package com.bachld.backend.repository;

import com.bachld.backend.dto.response.StudentLogResponse;
import com.bachld.backend.model.StudentLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StudentLogRepository extends JpaRepository<StudentLog, Integer> {

    @Query("""
        SELECT new com.bachld.backend.dto.response.StudentLogResponse(s.studentId, s.appLog, s.createdAt)
        FROM StudentLog s
        WHERE (:keyword IS NULL or LOWER(s.appLog) LIKE :keyword)
            AND s.studentId = :studentId
            AND s.status = 1
        ORDER BY s.createdAt DESC
    """)
    Page<StudentLogResponse> findByStudentIdAndKeyword(Pageable pageable, Integer studentId, String keyword);

}
