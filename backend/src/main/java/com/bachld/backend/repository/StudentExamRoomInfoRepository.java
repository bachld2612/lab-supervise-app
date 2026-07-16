package com.bachld.backend.repository;

import com.bachld.backend.dto.response.StudentAppUsageRaw;
import com.bachld.backend.model.StudentExamRoomInfo;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentExamRoomInfoRepository extends JpaRepository<StudentExamRoomInfo, Integer> {

  @Query(
      """
          SELECT new com.bachld.backend.dto.response.ClassStudentTrackingResponse(
              s.id, s.userId, u.fullName, s.code, u.email, u.phone, mc.id, mc.name
          )
          FROM StudentExamRoom ser
              JOIN Student s ON s.id = ser.studentId
              JOIN User u ON u.id = s.userId
              JOIN ManageClass mc ON mc.id = s.manageClassId
          WHERE ser.examRoomId = :examRoomId
          ORDER BY s.code ASC
      """)
  List<com.bachld.backend.dto.response.ClassStudentTrackingResponse> findStudentsWithLatestTracking(
      @Param("examRoomId") Integer examRoomId);

  @Query(
      """
          SELECT new com.bachld.backend.dto.response.StudentAppUsageRaw(
              s.id, seri.applicationName, seri.action, seri.clipboardTextEncrypted, seri.clipboardKeyEncrypted,
              seri.clipboardIv, seri.createdAt, seri.violation, seri.connectionType
          )
          FROM StudentExamRoomInfo seri
              JOIN StudentExamRoom ser ON ser.id = seri.studentExamRoomId
              JOIN Student s ON s.id = ser.studentId
          WHERE ser.examRoomId = :examRoomId
              AND seri.createdAt >= :startOfDay AND seri.createdAt < :endOfDay
          ORDER BY s.id ASC, seri.createdAt ASC
      """)
  List<StudentAppUsageRaw> findAppUsageByExamRoomIdAndDate(
      @Param("examRoomId") Integer examRoomId,
      @Param("startOfDay") LocalDateTime startOfDay,
      @Param("endOfDay") LocalDateTime endOfDay);
}
