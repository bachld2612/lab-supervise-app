package com.bachld.backend.repository;

import com.bachld.backend.model.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {

  Optional<RefreshToken> findByToken(String token);

  @Modifying
  @Transactional
  void deleteByToken(String token);

  @Modifying
  @Transactional
  void deleteByUserId(Integer userId);
}
