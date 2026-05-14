package com.bank_system.bank_system.repositories;

import com.bank_system.bank_system.models.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query(value = "SELECT * FROM transactions WHERE account_id = :accountId ORDER BY created_at DESC", nativeQuery = true)
    List<Transaction> findByAccountId(@Param("accountId") Long accountId);

    @Query(value = "SELECT t.* FROM transactions t " +
                   "INNER JOIN accounts a ON t.account_id = a.id " +
                   "WHERE a.customer_id = :customerId " +
                   "ORDER BY t.created_at DESC", nativeQuery = true)
    List<Transaction> findAllByCustomerId(@Param("customerId") Long customerId);

    @Modifying
    @Query(value = "INSERT INTO transactions (account_id, amount, transaction_type, created_at) " +
                   "VALUES (:accountId, :amount, :type, :createdAt)", nativeQuery = true)
    void saveTransaction(@Param("accountId") Long accountId,
                         @Param("amount") BigDecimal amount,
                         @Param("type") String type,
                         @Param("createdAt") LocalDateTime createdAt);
}
