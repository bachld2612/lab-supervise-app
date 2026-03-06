package com.bachld.backend.repository;

import com.bachld.backend.model.User;
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
            AND u.status = 1
    """)
    Optional<User> findByEmail(String email);
}
