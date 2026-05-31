package com.bachld.backend.repository;

import com.bachld.backend.dto.response.ClassResponse;
import com.bachld.backend.model.Classes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ClassRepository extends JpaRepository<Classes, Integer> {

    @Query("""
        SELECT new com.bachld.backend.dto.response.ClassResponse(
            c.id, c.name,
            (SELECT CAST(COUNT(stc.studentId) AS integer) FROM StudentClass stc WHERE stc.classId = c.id),
            c.maxStudent, c.sessionNumber, c.status, s.id, s.name, t.id, u.fullName, sc.id, sc.name, c.startDate, c.endDate,
            sm.id, sm.name, r.id, r.name, c.wifiSsid, c.trackingEnabled
        )
        FROM Classes c
            JOIN Semester sm ON sm.id = c.semesterId
            JOIN Subject s ON c.subjectId = s.id
            JOIN Teacher t ON c.teacherId = t.id
            JOIN User u ON t.userId = u.id
            JOIN Schedule sc ON c.scheduleId = sc.id
            LEFT JOIN Room r ON r.id = c.roomId
        WHERE (LOWER(c.name) LIKE :keyword)
            AND (:status IS NULL OR c.status = :status)
    """)
    Page<ClassResponse> findByKeyword(Pageable pageable, String keyword, Integer status);

    @Query("""
        SELECT new com.bachld.backend.dto.response.ClassResponse(
            c.id, c.name,
            (SELECT CAST(COUNT(stc.studentId) AS integer) FROM StudentClass stc WHERE stc.classId = c.id),
            c.maxStudent, c.sessionNumber, c.status, s.id, s.name, t.id, u.fullName, sc.id, sc.name, c.startDate, c.endDate,
            sm.id, sm.name, r.id, r.name, c.wifiSsid, c.trackingEnabled
        )
        FROM Classes c
            JOIN Semester sm ON sm.id = c.semesterId
            JOIN Subject s ON c.subjectId = s.id
            JOIN Teacher t ON c.teacherId = t.id
            JOIN User u ON t.userId = u.id
            JOIN Schedule sc ON c.scheduleId = sc.id
            LEFT JOIN Room r ON r.id = c.roomId
        WHERE c.id = :id
            AND c.status = :status
    """)
    ClassResponse findByIdAndStatus(Integer id, Integer status);

    @Query("""
        SELECT new com.bachld.backend.dto.response.ClassResponse(
            c.id, c.name,
            (SELECT CAST(COUNT(stc.studentId) AS integer) FROM StudentClass stc WHERE stc.classId = c.id),
            c.maxStudent, c.sessionNumber, c.status, s.id, s.name, t.id, u.fullName, sc.id, sc.name, c.startDate, c.endDate,
            sm.id, sm.name, r.id, r.name, c.wifiSsid, c.trackingEnabled
        )
        FROM Classes c
            JOIN Semester sm ON sm.id = c.semesterId
            JOIN Subject s ON c.subjectId = s.id
            JOIN Teacher t ON c.teacherId = t.id
            JOIN User u ON t.userId = u.id
            JOIN Schedule sc ON c.scheduleId = sc.id
            LEFT JOIN Room r ON r.id = c.roomId
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
            sm.id, sm.name, r.id, r.name, c.wifiSsid, c.trackingEnabled
        )
        FROM Classes c
            JOIN Semester sm ON sm.id = c.semesterId
            JOIN Subject s ON c.subjectId = s.id
            JOIN Teacher t ON c.teacherId = t.id
            JOIN User u ON t.userId = u.id
            JOIN Schedule sc ON c.scheduleId = sc.id
            LEFT JOIN Room r ON r.id = c.roomId
        WHERE t.userId = :userId
            AND :today BETWEEN c.startDate AND c.endDate
    """)
    List<ClassResponse> findActiveClassByTeacherUserId(Integer userId, LocalDate today);

    @Query("""
        SELECT new com.bachld.backend.dto.response.ClassResponse(
            c.id, c.name,
            (SELECT CAST(COUNT(stc.studentId) AS integer) FROM StudentClass stc WHERE stc.classId = c.id),
            c.maxStudent, c.sessionNumber, c.status, s.id, s.name, t.id, u.fullName, sc.id, sc.name, c.startDate, c.endDate,
            sm.id, sm.name, r.id, r.name, c.wifiSsid, c.trackingEnabled
        )
        FROM Classes c
            JOIN Semester sm ON sm.id = c.semesterId
            JOIN Subject s ON c.subjectId = s.id
            JOIN Teacher t ON c.teacherId = t.id
            JOIN User u ON t.userId = u.id
            JOIN Schedule sc ON c.scheduleId = sc.id
            LEFT JOIN Room r ON r.id = c.roomId
        WHERE c.status = 1
    """)
    List<ClassResponse> findAllActiveClasses();

    List<Classes> findByRoomIdAndStatus(Integer roomId, Integer status);

    @Query("""
        SELECT COUNT(c)
        FROM Classes c
            JOIN StudentClass stc ON stc.classId = c.id
            JOIN Student st ON st.id = stc.studentId
        WHERE st.userId = :userId
            AND :today BETWEEN c.startDate AND c.endDate
            AND c.status = 1
    """)
    Long countActiveClassesToday(@Param("userId") Integer userId, @Param("today") LocalDate today);

    @Query("""
        SELECT c.wifiSsid
        FROM Classes c
            JOIN StudentClass stc ON stc.classId = c.id
            JOIN Student st ON st.id = stc.studentId
        WHERE st.userId = :userId
            AND :today BETWEEN c.startDate AND c.endDate
            AND c.wifiSsid IS NOT NULL
        UNION
        SELECT er.wifiSsid
        FROM ExamRoom er
            JOIN StudentExamRoom ser ON ser.examRoomId = er.id
            JOIN Student st2 ON st2.id = ser.studentId
        WHERE st2.userId = :userId
            AND er.examDate = :today
            AND er.wifiSsid IS NOT NULL
            AND er.status = 1
            AND ser.status = 1
    """)
    List<String> findActiveWifiSsidsByStudentUserId(Integer userId, LocalDate today);
}
