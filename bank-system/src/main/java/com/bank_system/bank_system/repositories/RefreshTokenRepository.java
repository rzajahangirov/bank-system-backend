package com.bank_system.bank_system.repositories;

import com.bank_system.bank_system.models.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Query(value = "SELECT * FROM refresh_token WHERE token = :token", nativeQuery = true)
    Optional<RefreshToken> findByToken(@Param("token") String token);

    @Query(value = "SELECT * FROM refresh_token WHERE customer_id = :customerId", nativeQuery = true)
    Optional<RefreshToken> findByCustomerId(@Param("customerId") Long customerId);
}