package com.bachld.backend.repository;

import com.bachld.backend.dto.response.ExamScheduleView;
import com.bachld.backend.model.ExamRoom;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamRoomRepository extends JpaRepository<ExamRoom, Integer> {

  @Query(
      """
          SELECT er FROM ExamRoom er
              JOIN StudentExamRoom ser ON ser.examRoomId = er.id
          WHERE ser.studentId = :studentId
              AND er.examDate = :today
              AND er.status = 1
      """)
  List<ExamRoom> findActiveByStudentId(
      @Param("studentId") Integer studentId, @Param("today") LocalDate today);

  @Query(
      """
          SELECT new com.bachld.backend.dto.response.ExamScheduleView(
              er.examDate, er.startTime, er.endTime
          )
          FROM ExamRoom er
              JOIN StudentExamRoom ser ON ser.examRoomId = er.id
          WHERE ser.studentId = :studentId
              AND er.status = 1
              AND ser.status = 1
      """)
  List<ExamScheduleView> findExamSchedulesByStudentId(@Param("studentId") Integer studentId);

  @Query(
      """
          SELECT COUNT(er)
          FROM ExamRoom er
              JOIN StudentExamRoom ser ON ser.examRoomId = er.id
              JOIN Student st ON st.id = ser.studentId
          WHERE st.userId = :userId
              AND er.examDate = :today
              AND er.status = 1
              AND ser.status = 1
      """)
  Long countActiveExamsTodayByUserId(
      @Param("userId") Integer userId, @Param("today") LocalDate today);

  @Query(
      """
          SELECT er FROM ExamRoom er
          WHERE er.roomId = :roomId
              AND er.examDate = :examDate
              AND er.status = 1
      """)
  List<ExamRoom> findByRoomIdAndExamDate(
      @Param("roomId") Integer roomId, @Param("examDate") LocalDate examDate);

  @Query(
      """
          SELECT er FROM ExamRoom er
          WHERE er.examDate = :examDate
              AND er.status = 1
              AND (er.teacher1Id = :teacherId OR er.teacher2Id = :teacherId)
      """)
  List<ExamRoom> findByTeacherAndDate(
      @Param("teacherId") Integer teacherId, @Param("examDate") LocalDate examDate);

  @Query(
      """
          SELECT new com.bachld.backend.dto.response.ExamRoomResponse(
              er.id, er.code, er.roomId, r.name,
              er.teacher1Id, u1.fullName,
              er.teacher2Id, u2.fullName,
              er.subjectId, sub.name, er.semesterId, sem.name, er.maxStudent,
              (SELECT COUNT(ser2) FROM StudentExamRoom ser2 WHERE ser2.examRoomId = er.id AND ser2.status = 1),
              er.examDate, er.periods, er.startTime, er.endTime, er.status, er.trackingEnabled, er.wifiSsid
          )
          FROM ExamRoom er
              JOIN Room r ON r.id = er.roomId
              JOIN Teacher t1 ON t1.id = er.teacher1Id
              JOIN User u1 ON u1.id = t1.userId
              JOIN Teacher t2 ON t2.id = er.teacher2Id
              JOIN User u2 ON u2.id = t2.userId
              JOIN Subject sub ON sub.id = er.subjectId
              JOIN Semester sem ON sem.id = er.semesterId
          WHERE (:keyword IS NULL OR LOWER(er.code) LIKE :keyword OR LOWER(sub.name) LIKE :keyword)
              AND (:semesterId IS NULL OR er.semesterId = :semesterId)
              AND (:status IS NULL OR er.status = :status)
          ORDER BY er.examDate DESC, er.startTime ASC
      """)
  Page<com.bachld.backend.dto.response.ExamRoomResponse> findByKeyword(
      Pageable pageable,
      @Param("keyword") String keyword,
      @Param("semesterId") Integer semesterId,
      @Param("status") Integer status);

  @Query(
      """
          SELECT new com.bachld.backend.dto.response.ExamRoomResponse(
              er.id, er.code, er.roomId, r.name,
              er.teacher1Id, u1.fullName,
              er.teacher2Id, u2.fullName,
              er.subjectId, sub.name, er.semesterId, sem.name, er.maxStudent,
              (SELECT COUNT(ser2) FROM StudentExamRoom ser2 WHERE ser2.examRoomId = er.id AND ser2.status = 1),
              er.examDate, er.periods, er.startTime, er.endTime, er.status, er.trackingEnabled, er.wifiSsid
          )
          FROM ExamRoom er
              JOIN Room r ON r.id = er.roomId
              JOIN Teacher t1 ON t1.id = er.teacher1Id
              JOIN User u1 ON u1.id = t1.userId
              JOIN Teacher t2 ON t2.id = er.teacher2Id
              JOIN User u2 ON u2.id = t2.userId
              JOIN Subject sub ON sub.id = er.subjectId
              JOIN Semester sem ON sem.id = er.semesterId
          WHERE er.id = :id AND er.status = 1
      """)
  com.bachld.backend.dto.response.ExamRoomResponse findByIdProjected(@Param("id") Integer id);

  @Query(
      """
          SELECT new com.bachld.backend.dto.response.ExamRoomResponse(
              er.id, er.code, er.roomId, r.name,
              er.teacher1Id, u1.fullName,
              er.teacher2Id, u2.fullName,
              er.subjectId, sub.name, er.semesterId, sem.name, er.maxStudent,
              (SELECT COUNT(ser2) FROM StudentExamRoom ser2 WHERE ser2.examRoomId = er.id AND ser2.status = 1),
              er.examDate, er.periods, er.startTime, er.endTime, er.status, er.trackingEnabled, er.wifiSsid
          )
          FROM ExamRoom er
              JOIN StudentExamRoom ser ON ser.examRoomId = er.id
              JOIN Room r ON r.id = er.roomId
              JOIN Teacher t1 ON t1.id = er.teacher1Id
              JOIN User u1 ON u1.id = t1.userId
              JOIN Teacher t2 ON t2.id = er.teacher2Id
              JOIN User u2 ON u2.id = t2.userId
              JOIN Subject sub ON sub.id = er.subjectId
              JOIN Semester sem ON sem.id = er.semesterId
          WHERE ser.studentId = :studentId AND er.status = 1 AND ser.status = 1
          ORDER BY er.examDate ASC, er.startTime ASC
      """)
  List<com.bachld.backend.dto.response.ExamRoomResponse> findByStudent(
      @Param("studentId") Integer studentId);

  @Query(
      """
          SELECT new com.bachld.backend.dto.response.ExamRoomResponse(
              er.id, er.code, er.roomId, r.name,
              er.teacher1Id, u1.fullName,
              er.teacher2Id, u2.fullName,
              er.subjectId, sub.name, er.semesterId, sem.name, er.maxStudent,
              (SELECT COUNT(ser2) FROM StudentExamRoom ser2 WHERE ser2.examRoomId = er.id AND ser2.status = 1),
              er.examDate, er.periods, er.startTime, er.endTime, er.status, er.trackingEnabled, er.wifiSsid
          )
          FROM ExamRoom er
              JOIN Room r ON r.id = er.roomId
              JOIN Teacher t1 ON t1.id = er.teacher1Id
              JOIN User u1 ON u1.id = t1.userId
              JOIN Teacher t2 ON t2.id = er.teacher2Id
              JOIN User u2 ON u2.id = t2.userId
              JOIN Subject sub ON sub.id = er.subjectId
              JOIN Semester sem ON sem.id = er.semesterId
          WHERE er.status = 1
              AND (er.teacher1Id = :teacherId OR er.teacher2Id = :teacherId)
          ORDER BY er.examDate ASC, er.startTime ASC
      """)
  List<com.bachld.backend.dto.response.ExamRoomResponse> findByTeacher(
      @Param("teacherId") Integer teacherId);

  @Query(
      """
          SELECT new com.bachld.backend.dto.response.ExamRoomResponse(
              er.id, er.code, er.roomId, r.name,
              er.teacher1Id, u1.fullName,
              er.teacher2Id, u2.fullName,
              er.subjectId, sub.name, er.semesterId, sem.name, er.maxStudent,
              (SELECT COUNT(ser2) FROM StudentExamRoom ser2 WHERE ser2.examRoomId = er.id AND ser2.status = 1),
              er.examDate, er.periods, er.startTime, er.endTime, er.status, er.trackingEnabled, er.wifiSsid
          )
          FROM ExamRoom er
              JOIN Room r ON r.id = er.roomId
              JOIN Teacher t1 ON t1.id = er.teacher1Id
              JOIN User u1 ON u1.id = t1.userId
              JOIN Teacher t2 ON t2.id = er.teacher2Id
              JOIN User u2 ON u2.id = t2.userId
              JOIN Subject sub ON sub.id = er.subjectId
              JOIN Semester sem ON sem.id = er.semesterId
          WHERE er.status = 1
          ORDER BY er.examDate ASC, er.startTime ASC
      """)
  List<com.bachld.backend.dto.response.ExamRoomResponse> findAllActive();
}
