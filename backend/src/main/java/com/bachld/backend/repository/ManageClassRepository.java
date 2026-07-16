package com.bachld.backend.repository;

import com.bachld.backend.dto.response.ManageClassResponse;
import com.bachld.backend.model.ManageClass;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ManageClassRepository extends JpaRepository<ManageClass, Integer> {

  @Query(
      """
          SELECT new com.bachld.backend.dto.response.ManageClassResponse(
              mc.id, mc.name, mc.maxStudent, mc.status, u.fullName, t.id, mj.name, mj.id
          )
          FROM ManageClass mc
              JOIN Teacher t ON mc.teacherId = t.id
              JOIN User u ON t.userId = u.id
              JOIN Major mj ON mc.majorId = mj.id
          WHERE (LOWER(mc.name) LIKE :keyword)
              AND (:status IS NULL OR mc.status = :status)
          ORDER BY mc.updatedAt DESC
      """)
  Page<ManageClassResponse> findByKeyword(Pageable pageable, String keyword, Integer status);

  @Query(
      """
          SELECT new com.bachld.backend.dto.response.ManageClassResponse(
              mc.id, mc.name, mc.maxStudent, mc.status, u.fullName, t.id, mj.name, mj.id
          )
          FROM ManageClass mc
              JOIN Teacher t ON mc.teacherId = t.id
              JOIN User u ON t.userId = u.id
              JOIN Major mj ON mc.majorId = mj.id
          WHERE mc.id = :id
              AND mc.status = :status
      """)
  ManageClassResponse findByIdAndStatus(Integer id, Integer status);

  @Query(
      """
          SELECT mc
          FROM ManageClass mc
          WHERE mc.id = :id AND mc.status = :status
      """)
  Optional<ManageClass> findClassByIdAndStatus(Integer id, Integer status);

  @Query(
      """
          SELECT mc
          FROM ManageClass mc
          WHERE LOWER(mc.name) = LOWER(:name) AND mc.status = :status
      """)
  Optional<ManageClass> findClassByNameAndStatus(String name, Integer status);
}
