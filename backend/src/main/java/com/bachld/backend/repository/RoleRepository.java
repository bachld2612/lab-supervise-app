package com.bachld.backend.repository;

import com.bachld.backend.dto.response.RoleResponse;
import com.bachld.backend.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {

    @Query("""
        SELECT r
        FROM Role r
        WHERE r.id = :id
            AND r.status = :status
    """)
    Optional<Role> findByIdAndStatus(int id, int status);

    @Query("""
        SELECT new com.bachld.backend.dto.response.RoleResponse(r.id, r.name, r.vietnameseName)
        FROM Role r
        WHERE r.id = :id
    """)
    RoleResponse findRoleById(int id);
}
