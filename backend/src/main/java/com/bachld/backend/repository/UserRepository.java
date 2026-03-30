package com.bachld.backend.repository;

import com.bachld.backend.dto.response.UserResponse;
import com.bachld.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Integer> {

    @Query("""
        SELECT u
        FROM User u
        WHERE u.id = :id
            AND u.status = :status
    """)
    Optional<User> findByIdAndStatus(Integer id, Integer status);

    @Query("""
        SELECT u
        FROM User u
        WHERE u.email = :email
            AND u.status = :status
    """)
    Optional<User> findByEmailAndStatus(String email, Integer status);

    @Query("""
        SELECT u
        FROM User u
        WHERE u.phone = :phone
            AND u.status = :status
    """)
    Optional<User> findByPhoneAndStatus(String phone, Integer status);

    @Query("""
        SELECT new com.bachld.backend.dto.response.UserResponse(
            u.id, u.email, u.phone, u.fullName, u.hometown, u.birthday, u.rawPassword, r.id, r.name, r.color, u.status
        )
        FROM User u JOIN Role r ON u.roleId = r.id
        WHERE (LOWER(u.fullName) LIKE :keyword
                OR LOWER(u.email) LIKE :keyword
                OR LOWER(u.phone) LIKE :keyword
            )
            AND (:status IS NULL OR :status = u.status)
            AND (:roleType IS NULL OR :roleType = r.type)
            AND (:roleId IS NULL OR :roleId = r.id)
        ORDER BY u.updatedAt DESC
    """)
    Page<UserResponse> findAllByKeyword(Pageable pageable, String keyword, Integer status, Integer roleType, Integer roleId);

    @Query("""
        SELECT new com.bachld.backend.dto.response.UserResponse(
                    u.id, u.email, u.phone, u.fullName, u.hometown, u.birthday, u.rawPassword, r.id, r.name, r.color, r.status
        )
        FROM User u JOIN Role r ON u.roleId = r.id
        WHERE u.id = :id
            AND u.status = :status
    """)
    UserResponse findUserByIdAndStatus(int id, int status);
}
