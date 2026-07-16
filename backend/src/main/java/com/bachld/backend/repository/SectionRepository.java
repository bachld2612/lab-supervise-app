package com.bachld.backend.repository;

import com.bachld.backend.dto.response.SectionResponse;
import com.bachld.backend.model.Section;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SectionRepository extends JpaRepository<Section, Integer> {

  @Query(
      """
          SELECT new com.bachld.backend.dto.response.SectionResponse(s.id, s.name, s.status, d.name, d.id)
          FROM Section s
              JOIN Department d ON s.departmentId = d.id
          WHERE (LOWER(s.name) LIKE :keyword)
              AND (:status IS NULL OR s.status = :status)
      """)
  Page<SectionResponse> findByKeyword(Pageable pageable, String keyword, Integer status);

  @Query(
      """
          SELECT new com.bachld.backend.dto.response.SectionResponse(s.id, s.name, s.status, d.name, d.id)
          FROM Section s
              JOIN Department d ON s.departmentId = d.id
          WHERE s.id = :id
              AND s.status = :status
      """)
  SectionResponse findByIdAndStatus(Integer id, Integer status);
}
