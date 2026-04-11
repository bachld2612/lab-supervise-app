package com.bachld.backend.repository;

import com.bachld.backend.dto.response.ScheduleResponse;
import com.bachld.backend.model.Schedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ScheduleRepository extends JpaRepository<Schedule, Integer> {

    @Query("""
        SELECT new com.bachld.backend.dto.response.ScheduleResponse(
            s.id, s.name, s.daysOfWeek, s.periods, s.startTime, s.endTime, s.status
        )
        FROM Schedule s
        WHERE (LOWER(s.name) LIKE :keyword)
            AND (:status IS NULL OR s.status = :status)
    """)
    Page<ScheduleResponse> findByKeyword(Pageable pageable, String keyword, Integer status);

    @Query("""
        SELECT new com.bachld.backend.dto.response.ScheduleResponse(
            s.id, s.name, s.daysOfWeek, s.periods, s.startTime, s.endTime, s.status
        )
        FROM Schedule s
        WHERE s.id = :id
            AND s.status = :status
    """)
    ScheduleResponse findByIdAndStatus(Integer id, Integer status);
}
