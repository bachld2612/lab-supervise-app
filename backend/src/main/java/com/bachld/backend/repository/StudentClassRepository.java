package com.bachld.backend.repository;

import com.bachld.backend.model.Classes;
import com.bachld.backend.model.StudentClass;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface StudentClassRepository extends JpaRepository<StudentClass, Integer> {
    List<StudentClass> findByStudentId(Integer studentId);

    @Query("""
        SELECT c
        FROM Classes c
            JOIN StudentClass sc ON c.id = sc.classId
        WHERE sc.studentId = :studentId
            AND :now BETWEEN c.startDate AND c.endDate
    """)
    List<Classes> findActiveClassesByStudentId(Integer studentId, LocalDate now);

    Optional<StudentClass> findByStudentIdAndClassId(Integer studentId, Integer classId);
}
