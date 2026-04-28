package com.bachld.backend.repository;

import com.bachld.backend.dto.response.ClassStudentTrackingResponse;
import com.bachld.backend.dto.response.StudentAppUsageRaw;
import com.bachld.backend.model.StudentClassInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface StudentClassInfoRepository extends JpaRepository<StudentClassInfo,Integer> {

    @Query("""
        SELECT new com.bachld.backend.dto.response.ClassStudentTrackingResponse(s.id, u.fullName, s.code, u.email, u.phone, mc.id, mc.name)
        FROM StudentClass stc
            JOIN Student s ON s.id = stc.studentId
            JOIN User u ON u.id = s.userId
            JOIN ManageClass mc ON mc.id = s.manageClassId
        WHERE stc.classId = :classId
        ORDER BY s.code ASC
    """)
    List<ClassStudentTrackingResponse> findStudentsWithLatestTracking(@Param("classId") Integer classId);

    @Query("""
        SELECT new com.bachld.backend.dto.response.StudentAppUsageRaw(s.id, sci.applicationName, sci.createdAt, sci.isBanApplication)
        FROM StudentClassInfo sci
            JOIN StudentClass stc ON stc.id = sci.studentClassId
            JOIN Student s ON s.id = stc.studentId
        WHERE stc.classId = :classId
            AND FUNCTION('DATE', sci.createdAt) = :date
        ORDER BY s.id ASC, sci.createdAt ASC
    """)
    List<StudentAppUsageRaw> findAppUsageByClassIdAndDate(
            @Param("classId") Integer classId,
            @Param("date") LocalDate date);
}
