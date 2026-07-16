package com.bachld.backend.repository;

import com.bachld.backend.dto.response.RoomResponse;
import com.bachld.backend.model.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RoomRepository extends JpaRepository<Room, Integer> {

  @Query(
      """
          SELECT new com.bachld.backend.dto.response.RoomResponse(r.id, r.name, r.capacity, r.status)
          FROM Room r
          WHERE (LOWER(r.name) LIKE :keyword)
              AND (:status IS NULL OR r.status = :status)
      """)
  Page<RoomResponse> findByKeyword(Pageable pageable, String keyword, Integer status);

  @Query(
      """
          SELECT new com.bachld.backend.dto.response.RoomResponse(r.id, r.name, r.capacity, r.status)
          FROM Room r
          WHERE r.id = :id
              AND r.status = :status
      """)
  RoomResponse findByIdAndStatus(Integer id, Integer status);
}
