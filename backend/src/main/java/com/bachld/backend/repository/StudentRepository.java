package com.bachld.backend.repository;

import com.bachld.backend.dto.response.StudentResponse;
import com.bachld.backend.model.Student;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StudentRepository extends JpaRepository<Student, Integer> {
  Optional<Student> findByUserId(Integer userId);

  @Query(
      """
          SELECT s
          FROM Student s
              JOIN User u ON s.userId = u.id
          WHERE s.code = :code
              AND u.status = :status
      """)
  Optional<Student> findByCodeAndStatus(String code, int status);

  @Query(
      """
          SELECT new com.bachld.backend.dto.response.StudentResponse(
              s.id, u.email, u.phone, u.fullName, s.code, mc.id, mc.name, u.hometown, u.birthday, u.rawPassword, u.status
          )
          FROM Student s JOIN User u ON s.userId = u.id
              JOIN ManageClass mc ON mc.id = s.manageClassId
          WHERE (LOWER(u.fullName) LIKE :keyword
                  OR LOWER(u.email) LIKE :keyword
                  OR LOWER(u.phone) LIKE :keyword
                  OR LOWER(s.code) LIKE :keyword
              )
              AND (:manageClassId IS NULL OR :manageClassId = s.manageClassId)
              AND (:status IS NULL OR :status = u.status)
          ORDER BY s.code ASC
      """)
  Page<StudentResponse> findByKeyword(
      Pageable pageable, String keyword, Integer status, Integer manageClassId);

  @Query(
      """
          SELECT new com.bachld.backend.dto.response.StudentResponse(
              s.id, u.email, u.phone, u.fullName, s.code, mc.id, mc.name, u.hometown, u.birthday, u.rawPassword, u.status
          )
          FROM Student s JOIN User u ON s.userId = u.id
              JOIN ManageClass mc ON mc.id = s.manageClassId
          WHERE s.id = :id
              AND u.status = :status
      """)
  StudentResponse findStudentByIdAndStatus(int id, int status);

  @Query(
      """
          SELECT new com.bachld.backend.dto.response.StudentResponse(
              s.id, u.email, u.phone, u.fullName, s.code, mc.id, mc.name, u.hometown, u.birthday, u.rawPassword, u.status
          )
          FROM StudentClass stc
              JOIN Student s ON s.id = stc.studentId
              JOIN User u ON u.id = s.userId
              JOIN ManageClass mc ON mc.id = s.manageClassId
          WHERE stc.classId = :classId
              AND (LOWER(u.fullName) LIKE :keyword
                  OR LOWER(u.email) LIKE :keyword
                  OR LOWER(u.phone) LIKE :keyword
              )
          ORDER BY s.code ASC
      """)
  Page<StudentResponse> findByClassId(Pageable pageable, Integer classId, String keyword);

  @Query(
      """
          SELECT new com.bachld.backend.dto.response.StudentResponse(
              s.id, u.email, u.phone, u.fullName, s.code, mc.id, mc.name, u.hometown, u.birthday, u.rawPassword, u.status
          )
          FROM Student s JOIN User u ON s.userId = u.id
              JOIN ManageClass mc ON mc.id = s.manageClassId
          WHERE u.status = :status
              AND (LOWER(u.fullName) LIKE :keyword
                  OR LOWER(u.email) LIKE :keyword
                  OR LOWER(u.phone) LIKE :keyword
                  OR LOWER(s.code) LIKE :keyword
              )
              AND s.id NOT IN (
                  SELECT stc.studentId FROM StudentClass stc WHERE stc.classId = :classId
              )
          ORDER BY s.code ASC
      """)
  Page<StudentResponse> findStudentsNotInClass(
      Pageable pageable, Integer classId, String keyword, int status);

  @Query(
      """
          SELECT new com.bachld.backend.dto.response.StudentResponse(
              s.id, u.email, u.phone, u.fullName, s.code, mc.id, mc.name, u.hometown, u.birthday, u.rawPassword, u.status
          )
          FROM StudentExamRoom ser
              JOIN Student s ON s.id = ser.studentId
              JOIN User u ON u.id = s.userId
              JOIN ManageClass mc ON mc.id = s.manageClassId
          WHERE ser.examRoomId = :examRoomId
              AND ser.status = 1
              AND (LOWER(u.fullName) LIKE :keyword
                  OR LOWER(u.email) LIKE :keyword
                  OR LOWER(u.phone) LIKE :keyword
              )
          ORDER BY s.code ASC
      """)
  Page<StudentResponse> findByExamRoomId(Pageable pageable, Integer examRoomId, String keyword);

  @Query(
      """
          SELECT new com.bachld.backend.dto.response.StudentResponse(
              s.id, u.email, u.phone, u.fullName, s.code, mc.id, mc.name, u.hometown, u.birthday, u.rawPassword, u.status
          )
          FROM Student s JOIN User u ON s.userId = u.id
              JOIN ManageClass mc ON mc.id = s.manageClassId
          WHERE u.status = :status
              AND (LOWER(u.fullName) LIKE :keyword
                  OR LOWER(u.email) LIKE :keyword
                  OR LOWER(u.phone) LIKE :keyword
                  OR LOWER(s.code) LIKE :keyword
              )
              AND s.id NOT IN (
                  SELECT ser.studentId FROM StudentExamRoom ser WHERE ser.examRoomId = :examRoomId AND ser.status = 1
              )
          ORDER BY s.code ASC
      """)
  Page<StudentResponse> findStudentsNotInExamRoom(
      Pageable pageable, Integer examRoomId, String keyword, int status);

  @Query(
      """
          SELECT COUNT(s)
          FROM Student s
              JOIN User u ON s.userId = u.id
          WHERE s.manageClassId = :manageClassId
              AND u.status = :status
      """)
  long countByManageClassIdAndStatus(Integer manageClassId, int status);
}
