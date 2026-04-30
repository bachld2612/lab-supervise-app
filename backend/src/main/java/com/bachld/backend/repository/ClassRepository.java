package com.bachld.backend.repository;

import com.bachld.backend.dto.response.ClassResponse;
import com.bachld.backend.model.Classes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface ClassRepository extends JpaRepository<Classes, Integer> {

    @Query("""
        SELECT new com.bachld.backend.dto.response.ClassResponse(
            c.id, c.name,
            (SELECT CAST(COUNT(stc.studentId) AS integer) FROM StudentClass stc WHERE stc.classId = c.id),
            c.maxStudent, c.sessionNumber, c.status, s.id, s.name, t.id, u.fullName, sc.id, sc.name, c.startDate, c.endDate,
            sm.id, sm.name
        )
        FROM Classes c
            JOIN Semester sm ON sm.id = c.semesterId
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
            c.maxStudent, c.sessionNumber, c.status, s.id, s.name, t.id, u.fullName, sc.id, sc.name, c.startDate, c.endDate,
            sm.id, sm.name
        )
        FROM Classes c
            JOIN Semester sm ON sm.id = c.semesterId
            JOIN Subject s ON c.subjectId = s.id
            JOIN Teacher t ON c.teacherId = t.id
            JOIN User u ON t.userId = u.id
            JOIN Schedule sc ON c.scheduleId = sc.id
        WHERE c.id = :id
            AND c.status = :status
    """)
    ClassResponse findByIdAndStatus(Integer id, Integer status);

    @Query("""
        SELECT new com.bachld.backend.dto.response.ClassResponse(
            c.id, c.name,
            (SELECT CAST(COUNT(stc.studentId) AS integer) FROM StudentClass stc WHERE stc.classId = c.id),
            c.maxStudent, c.sessionNumber, c.status, s.id, s.name, t.id, u.fullName, sc.id, sc.name, c.startDate, c.endDate,
            sm.id, sm.name
        )
        FROM Classes c
            JOIN Semester sm ON sm.id = c.semesterId
            JOIN Subject s ON c.subjectId = s.id
            JOIN Teacher t ON c.teacherId = t.id
            JOIN User u ON t.userId = u.id
            JOIN Schedule sc ON c.scheduleId = sc.id
            JOIN StudentClass stc ON stc.classId = c.id
            JOIN Student st ON st.id = stc.studentId
        WHERE st.userId = :userId
            AND :today BETWEEN c.startDate AND c.endDate
    """)
    List<ClassResponse> findActiveClassByStudentUserId(Integer userId, LocalDate today);

    @Query("""
        SELECT new com.bachld.backend.dto.response.ClassResponse(
            c.id, c.name,
            (SELECT CAST(COUNT(stc.studentId) AS integer) FROM StudentClass stc WHERE stc.classId = c.id),
            c.maxStudent, c.sessionNumber, c.status, s.id, s.name, t.id, u.fullName, sc.id, sc.name, c.startDate, c.endDate,
            sm.id, sm.name
        )
        FROM Classes c
            JOIN Semester sm ON sm.id = c.semesterId
            JOIN Subject s ON c.subjectId = s.id
            JOIN Teacher t ON c.teacherId = t.id
            JOIN User u ON t.userId = u.id
            JOIN Schedule sc ON c.scheduleId = sc.id
        WHERE t.userId = :userId
            AND :today BETWEEN c.startDate AND c.endDate
    """)
    List<ClassResponse> findActiveClassByTeacherUserId(Integer userId, LocalDate today);
}
