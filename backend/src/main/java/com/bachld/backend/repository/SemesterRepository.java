package com.bachld.backend.repository;

import com.bachld.backend.dto.response.SemesterResponse;
import com.bachld.backend.model.Semester;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SemesterRepository extends JpaRepository<Semester, Integer> {

    @Query("""
        SELECT new com.bachld.backend.dto.response.SemesterResponse(
            s.id, s.name, s.studyYear, s.startDate, s.endDate, s.status
        )
        FROM Semester s
        WHERE (LOWER(s.name) LIKE :keyword OR LOWER(s.studyYear) LIKE :keyword)
            AND (:status IS NULL OR s.status = :status)
    """)
    Page<SemesterResponse> findByKeyword(Pageable pageable, String keyword, Integer status);

    @Query("""
        SELECT new com.bachld.backend.dto.response.SemesterResponse(
            s.id, s.name, s.studyYear, s.startDate, s.endDate, s.status
        )
        FROM Semester s
        WHERE s.id = :id
            AND s.status = :status
    """)
    SemesterResponse findByIdAndStatusExtended(Integer id, Integer status);
}
