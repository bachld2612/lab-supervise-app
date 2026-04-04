package com.bachld.backend.repository;

import com.bachld.backend.dto.response.MajorResponse;
import com.bachld.backend.model.Major;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MajorRepository extends JpaRepository<Major,Integer> {

    @Query("""
        SELECT new com.bachld.backend.dto.response.MajorResponse(m.id, m.name, m.status, d.name, d.id)
        FROM Major m
            JOIN Department d ON m.departmentId = d.id
        WHERE (LOWER(m.name) LIKE :keyword)
            AND (:status IS NULL OR m.status = :status)
    """)
    Page<MajorResponse> findByKeyword(Pageable pageable, String keyword, Integer status);

    @Query("""
        SELECT new com.bachld.backend.dto.response.MajorResponse(m.id, m.name, m.status, d.name, d.id)
        FROM Major m
            JOIN Department d ON m.departmentId = d.id
        WHERE m.id = :id
            AND m.status = :status
    """)
    MajorResponse findByIdAndStatus(Integer id, Integer status);
}
