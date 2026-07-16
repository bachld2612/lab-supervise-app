package com.bachld.backend.repository;

import com.bachld.backend.dto.response.DepartmentResponse;
import com.bachld.backend.model.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DepartmentRepository extends JpaRepository<Department, Integer> {

  @Query(
      """
          SELECT new com.bachld.backend.dto.response.DepartmentResponse(d.id, d.name, d.status)
          FROM Department d
          WHERE (LOWER(d.name) LIKE :keyword)
              AND (:status IS NULL OR d.status = :status)
      """)
  Page<DepartmentResponse> findByKeyword(Pageable pageable, String keyword, Integer status);

  @Query(
      """
          SELECT new com.bachld.backend.dto.response.DepartmentResponse(d.id, d.name, d.status)
          FROM Department d
          WHERE d.id = :id
              AND d.status = :status
      """)
  DepartmentResponse findByIdAndStatus(Integer id, Integer status);
}
