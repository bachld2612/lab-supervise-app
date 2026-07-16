package com.bachld.backend.repository;

import com.bachld.backend.dto.response.IncidentReportResponse;
import com.bachld.backend.model.IncidentReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface IncidentReportRepository extends JpaRepository<IncidentReport, Integer> {

  @Query(
      """
          SELECT new com.bachld.backend.dto.response.IncidentReportResponse(
              ir.id, ir.title, ir.status,
              ir.roomId, r.name,
              ir.reporterId, ru.fullName, ir.reporterRole,
              ir.handlerId, hu.fullName,
              ir.createdAt
          )
          FROM IncidentReport ir
          LEFT JOIN Room r ON r.id = ir.roomId
          JOIN User ru ON ru.id = ir.reporterId
          LEFT JOIN User hu ON hu.id = ir.handlerId
          WHERE (LOWER(ir.title) LIKE :keyword)
              AND (:status IS NULL OR ir.status = :status)
              AND (:roomId IS NULL OR ir.roomId = :roomId)
              AND (:reporterId IS NULL OR ir.reporterId = :reporterId)
          ORDER BY ir.createdAt DESC
      """)
  Page<IncidentReportResponse> findAllByKeyword(
      Pageable pageable, String keyword, Integer status, Integer roomId, Integer reporterId);
}
