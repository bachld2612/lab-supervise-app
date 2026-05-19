package com.bachld.backend.repository;

import com.bachld.backend.model.StudentExamRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudentExamRoomRepository extends JpaRepository<StudentExamRoom, Integer> {

    Optional<StudentExamRoom> findByStudentIdAndExamRoomId(Integer studentId, Integer examRoomId);

    List<StudentExamRoom> findByExamRoomId(Integer examRoomId);

    long countByExamRoomIdAndStatus(Integer examRoomId, int status);

    @Query("""
        SELECT ser FROM StudentExamRoom ser
            JOIN ExamRoom er ON er.id = ser.examRoomId
        WHERE ser.studentId = :studentId
            AND er.examDate = :examDate
            AND er.status = 1
            AND ser.status = 1
    """)
    List<StudentExamRoom> findByStudentIdAndExamDate(
            @Param("studentId") Integer studentId,
            @Param("examDate") LocalDate examDate);
}