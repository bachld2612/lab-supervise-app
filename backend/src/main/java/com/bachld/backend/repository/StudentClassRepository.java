package com.bachld.backend.repository;

import com.bachld.backend.dto.response.ClassScheduleView;
import com.bachld.backend.model.Classes;
import com.bachld.backend.model.StudentClass;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface StudentClassRepository extends JpaRepository<StudentClass, Integer> {
  List<StudentClass> findByStudentId(Integer studentId);

  @Modifying
  @Transactional
  void deleteByClassIdAndStudentIdIn(Integer classId, List<Integer> studentIds);

  @Query(
      """
          SELECT c
          FROM Classes c
              JOIN StudentClass sc ON c.id = sc.classId
          WHERE sc.studentId = :studentId
              AND :now BETWEEN c.startDate AND c.endDate
      """)
  List<Classes> findActiveClassesByStudentId(Integer studentId, LocalDate now);

  Optional<StudentClass> findByStudentIdAndClassId(Integer studentId, Integer classId);

  long countByClassId(Integer classId);

  List<StudentClass> findByClassId(Integer classId);

  @Query(
      """
          SELECT new com.bachld.backend.dto.response.ClassScheduleView(
              c.startDate, c.endDate, sc.daysOfWeek, sc.startTime, sc.endTime
          )
          FROM StudentClass stc
              JOIN Classes c ON c.id = stc.classId
              JOIN Schedule sc ON sc.id = c.scheduleId
          WHERE stc.studentId = :studentId
              AND c.id <> :excludeClassId
              AND c.status = 1
      """)
  List<ClassScheduleView> findOtherClassSchedulesByStudentId(
      Integer studentId, Integer excludeClassId);

  @Query(
      """
          SELECT new com.bachld.backend.dto.response.ClassScheduleView(
              c.startDate, c.endDate, sc.daysOfWeek, sc.startTime, sc.endTime
          )
          FROM StudentClass stc
              JOIN Classes c ON c.id = stc.classId
              JOIN Schedule sc ON sc.id = c.scheduleId
          WHERE stc.studentId = :studentId
              AND c.status = 1
      """)
  List<ClassScheduleView> findClassSchedulesByStudentId(Integer studentId);
}
