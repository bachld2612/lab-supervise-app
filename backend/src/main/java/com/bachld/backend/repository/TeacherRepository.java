package com.bachld.backend.repository;

import com.bachld.backend.dto.response.TeacherResponse;
import com.bachld.backend.model.Teacher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Integer> {

    @Query("""
        SELECT t
        FROM Teacher t
            JOIN User u ON t.userId = u.id
        WHERE t.code = :code
            AND u.status = :status
    """)
    Optional<Teacher> findByCodeAndStatus(String code, int status);

    @Query("""
        SELECT new com.bachld.backend.dto.response.TeacherResponse(
            t.id, u.email, u.phone, u.fullName, t.code, u.hometown, u.birthday, u.rawPassword, u.status
        )
        FROM Teacher t JOIN User u ON t.userId = u.id
        WHERE (LOWER(u.fullName) LIKE :keyword
                OR LOWER(u.email) LIKE :keyword
                OR LOWER(u.phone) LIKE :keyword
                OR LOWER(t.code) LIKE :keyword
            )
            AND (:status IS NULL OR :status = u.status)
    """)
    Page<TeacherResponse> findByKeyword(Pageable pageable, String keyword, Integer status);

    @Query("""
        SELECT new com.bachld.backend.dto.response.TeacherResponse(
            t.id, u.email, u.phone, u.fullName, t.code, u.hometown, u.birthday, u.rawPassword, u.status
        )
        FROM Teacher t JOIN User u ON t.userId = u.id
        WHERE t.id = :id
            AND u.status = :status
    """)
    TeacherResponse findTeacherByIdAndStatus(int id, int status);
}
