package com.bachld.backend.repository;

import com.bachld.backend.model.PersonalComputer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PersonalComputerRepository extends JpaRepository<PersonalComputer, Integer> {
  Optional<PersonalComputer> findByUserId(Integer userId);

  @Query(
      """
          SELECT pc
          FROM PersonalComputer pc JOIN User u ON pc.userId = u.id
          WHERE pc.ipAddress = :ipAddress
              AND u.roleId = :roleId
      """)
  Optional<PersonalComputer> findByIpAddressAndRoleId(String ipAddress, Integer roleId);
}
