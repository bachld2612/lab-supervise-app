package com.bachld.backend.repository;

import com.bachld.backend.dto.response.StudentResponse;
import com.bachld.backend.model.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Integer> {

    @Query("""
        SELECT s
        FROM Student s
        WHERE s.id = :id
            AND s.status = :status
    """)
    Optional<Student> findStudentByIdAndStatus(Integer id, int status);

    @Query("""
        SELECT new com.bachld.backend.dto.response.StudentResponse(s.id, s.fullName, s.hostname, s.password, s.username)
        FROM Student s
        WHERE s.id = :id
            AND s.status = :status
    """)
    Optional<StudentResponse> findByIdAndStatus(Integer id, int status);

    @Query("""
        SELECT new com.bachld.backend.dto.response.StudentResponse(s.id, s.fullName, s.hostname, s.password, s.username)
        FROM Student s
        WHERE LOWER(s.fullName) like :keyword
            AND (:status IS NULL OR s.status = :status)
        ORDER BY s.createdAt DESC
    """)
    Page<StudentResponse> findAllByKeyword(Pageable pageable, String keyword, Integer status);
}
