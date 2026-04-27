package com.bachld.backend.repository;

import com.bachld.backend.dto.response.BanApplicationResponse;
import com.bachld.backend.model.BanApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BanApplicationRepository extends JpaRepository<BanApplication, Integer> {

    @Query("""
        SELECT new com.bachld.backend.dto.response.BanApplicationResponse(
            b.id, b.teacherId, b.applicationName, b.imageUrl, b.status
        )
        FROM BanApplication b
        WHERE b.teacherId = :teacherId
            AND LOWER(b.applicationName) LIKE :keyword
            AND b.status = 1
        ORDER BY b.applicationName ASC
    """)
    Page<BanApplicationResponse> findByKeywordAndTeacherId(Pageable pageable, String keyword, Integer teacherId);

    @Query("""
        SELECT new com.bachld.backend.dto.response.BanApplicationResponse(
            b.id, b.teacherId, b.applicationName, b.imageUrl, b.status
        )
        FROM BanApplication b
        WHERE b.id = :id
            AND b.teacherId = :teacherId
            AND b.status = 1
    """)
    BanApplicationResponse findByIdAndTeacherId(Integer id, Integer teacherId);
}