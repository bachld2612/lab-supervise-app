package com.bachld.backend.repository;

import com.bachld.backend.model.PersonalComputer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonalComputerRepository extends JpaRepository<PersonalComputer, Integer> {
    Optional<PersonalComputer> findByUserId(Integer userId);
    Optional<PersonalComputer> findByIpAddress(String ipAddress);
}
