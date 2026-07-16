package com.bachld.backend.repository;

import com.bachld.backend.dto.response.TeacherResponse;
import com.bachld.backend.model.Teacher;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TeacherRepository extends JpaRepository<Teacher, Integer> {
  Optional<Teacher> findByUserId(Integer userId);

  @Query(
      """
          SELECT t
          FROM Teacher t
              JOIN User u ON t.userId = u.id
          WHERE t.code = :code
              AND u.status = :status
      """)
  Optional<Teacher> findByCodeAndStatus(String code, int status);

  @Query(
      """
          SELECT new com.bachld.backend.dto.response.TeacherResponse(
              t.id, u.email, u.phone, u.fullName, t.code, u.hometown, s.id, s.name, u.birthday, u.id, u.rawPassword, u.status
          )
          FROM Teacher t JOIN User u ON t.userId = u.id
              JOIN Section s ON t.sectionId = s.id
          WHERE (LOWER(u.fullName) LIKE :keyword
                  OR LOWER(u.email) LIKE :keyword
                  OR LOWER(u.phone) LIKE :keyword
                  OR LOWER(t.code) LIKE :keyword
              )
              AND (:status IS NULL OR :status = u.status)
          ORDER BY t.updatedAt DESC
      """)
  Page<TeacherResponse> findByKeyword(Pageable pageable, String keyword, Integer status);

  @Query(
      """
          SELECT new com.bachld.backend.dto.response.TeacherResponse(
              t.id, u.email, u.phone, u.fullName, t.code, u.hometown, s.id, s.name, u.birthday, u.id, u.rawPassword, u.status
          )
          FROM Teacher t JOIN User u ON t.userId = u.id
              JOIN Section s ON t.sectionId = s.id
          WHERE t.id = :id
              AND u.status = :status
      """)
  TeacherResponse findTeacherByIdAndStatus(int id, int status);
}
