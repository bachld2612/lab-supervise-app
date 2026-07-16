package com.bachld.backend.repository;

import com.bachld.backend.model.AllowedApplication;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AllowedApplicationRepository extends JpaRepository<AllowedApplication, Integer> {

  @Query(
      """
          SELECT a.applicationName FROM AllowedApplication a
          WHERE a.examRoomId = :examRoomId AND a.status = 1
      """)
  List<String> findActiveAppNamesByExamRoomId(@Param("examRoomId") Integer examRoomId);

  List<AllowedApplication> findByExamRoomIdAndStatus(Integer examRoomId, int status);

  @Query(
      """
          SELECT new com.bachld.backend.dto.response.AllowedApplicationResponse(
              a.id, a.examRoomId, a.applicationName, a.imageUrl, a.status
          )
          FROM AllowedApplication a
          WHERE a.examRoomId = :examRoomId
              AND (:keyword IS NULL OR LOWER(a.applicationName) LIKE :keyword)
              AND (:status IS NULL OR a.status = :status)
          ORDER BY a.applicationName ASC
      """)
  org.springframework.data.domain.Page<com.bachld.backend.dto.response.AllowedApplicationResponse>
      findByExamRoomAndKeyword(
          org.springframework.data.domain.Pageable pageable,
          @Param("examRoomId") Integer examRoomId,
          @Param("keyword") String keyword,
          @Param("status") Integer status);
}
