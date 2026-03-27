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
            JOIN User u ON s.userId = u.id
        WHERE s.code = :code
            AND u.status = :status
    """)
    Optional<Student> findByCodeAndStatus(String code, int status);

    @Query("""
        SELECT new com.bachld.backend.dto.response.StudentResponse(
            u.email, u.phone, u.fullName, s.code, u.hometown, u.birthday, u.rawPassword, u.status
        )
        FROM Student s JOIN User u ON s.userId = u.id
        WHERE (LOWER(u.fullName) LIKE :keyword
                OR LOWER(u.email) LIKE :keyword
                OR LOWER(u.phone) LIKE :keyword
                OR LOWER(s.code) LIKE :keyword
            )
            AND (:status IS NULL OR :status = u.status)
    """)
    Page<StudentResponse> findByKeyword(Pageable pageable, String keyword, Integer status);

    @Query("""
        SELECT new com.bachld.backend.dto.response.StudentResponse(
            u.email, u.phone, u.fullName, s.code, u.hometown, u.birthday, u.rawPassword, u.status
        )
        FROM Student s JOIN User u ON s.userId = u.id
        WHERE s.id = :id
            AND u.status = :status
    """)
    StudentResponse findTeacherByIdAndStatus(int id, int status);
}
