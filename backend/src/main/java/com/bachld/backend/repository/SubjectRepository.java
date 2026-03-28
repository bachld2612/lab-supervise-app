package com.bachld.backend.repository;

import com.bachld.backend.dto.response.SubjectResponse;
import com.bachld.backend.model.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Integer> {

    @Query("""
        SELECT new com.bachld.backend.dto.response.SubjectResponse(
            s.id, s.name, s.code, s.creditNumber, s.status, sec.name
        )
        FROM Subject s
            JOIN Section sec ON s.sectionId = sec.id
        WHERE (LOWER(s.name) LIKE :keyword OR LOWER(s.code) LIKE :keyword)
            AND (:status IS NULL OR s.status = :status)
    """)
    Page<SubjectResponse> findByKeyword(Pageable pageable, String keyword, Integer status);

    @Query("""
        SELECT new com.bachld.backend.dto.response.SubjectResponse(
            s.id, s.name, s.code, s.creditNumber, s.status, sec.name
        )
        FROM Subject s
            JOIN Section sec ON s.sectionId = sec.id
        WHERE s.id = :id
            AND s.status = :status
    """)
    SubjectResponse findByIdAndStatusExtended(Integer id, Integer status);

    @Query("""
        SELECT s
        FROM Subject s
        WHERE s.code = :code
            AND s.status = :status
    """)
    Optional<Subject> findByCodeAndStatus(String code, Integer status);
}
