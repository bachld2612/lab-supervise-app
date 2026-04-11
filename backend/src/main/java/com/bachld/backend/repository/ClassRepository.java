package com.bachld.backend.repository;

import com.bachld.backend.dto.response.ClassResponse;
import com.bachld.backend.model.Classes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ClassRepository extends JpaRepository<Classes, Integer> {

    @Query("""
        SELECT new com.bachld.backend.dto.response.ClassResponse(
            c.id, c.name,
            (SELECT CAST(COUNT(stc.studentId) AS integer) FROM StudentClass stc WHERE stc.classId = c.id),
            c.maxStudent, c.sessionNumber, c.status, s.id, s.name, t.id, u.fullName, sc.id, sc.name
        )
        FROM Classes c
            JOIN Subject s ON c.subjectId = s.id
            JOIN Teacher t ON c.teacherId = t.id
            JOIN User u ON t.userId = u.id
            JOIN Schedule sc ON c.scheduleId = sc.id
        WHERE (LOWER(c.name) LIKE :keyword)
            AND (:status IS NULL OR c.status = :status)
    """)
    Page<ClassResponse> findByKeyword(Pageable pageable, String keyword, Integer status);

    @Query("""
        SELECT new com.bachld.backend.dto.response.ClassResponse(
            c.id, c.name,
            (SELECT CAST(COUNT(stc.studentId) AS integer) FROM StudentClass stc WHERE stc.classId = c.id),
            c.maxStudent, c.sessionNumber, c.status, s.id, s.name, t.id, u.fullName, sc.id, sc.name
        )
        FROM Classes c
            JOIN Subject s ON c.subjectId = s.id
            JOIN Teacher t ON c.teacherId = t.id
            JOIN User u ON t.userId = u.id
            JOIN Schedule sc ON c.scheduleId = sc.id
        WHERE c.id = :id
            AND c.status = :status
    """)
    ClassResponse findByIdAndStatus(Integer id, Integer status);
}
