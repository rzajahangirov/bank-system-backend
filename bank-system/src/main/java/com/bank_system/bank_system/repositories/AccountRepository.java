package com.bank_system.bank_system.repositories;

import com.bank_system.bank_system.models.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    @Query(value = "SELECT * FROM accounts WHERE customer_id = :customerId", nativeQuery = true)
    List<Account> findByCustomerId(@Param("customerId") Long customerId);

    @Query(value = "SELECT * FROM accounts WHERE account_number = :accountNumber", nativeQuery = true)
    Optional<Account> findByAccountNumber(@Param("accountNumber") String accountNumber);

    @Query(value = "SELECT * FROM accounts WHERE id = :id", nativeQuery = true)
    Optional<Account> findByIdNative(@Param("id") Long id);

    @Modifying
    @Query(value = "UPDATE accounts SET balance = balance + :amount WHERE account_number = :accountNumber", nativeQuery = true)
    void updateBalanceByAccountNumber(@Param("accountNumber") String accountNumber, @Param("amount") BigDecimal amount);

    @Modifying
    @Query(value = "UPDATE accounts SET balance = balance + :amount WHERE id = :accountId", nativeQuery = true)
    void updateBalanceById(@Param("accountId") Long accountId, @Param("amount") BigDecimal amount);

    @Modifying
    @Query(value = "UPDATE accounts SET status = :status WHERE id = :id", nativeQuery = true)
    void updateStatus(@Param("id") Long id, @Param("status") String status);

    @Modifying
    @Query(value = "INSERT INTO accounts (customer_id, account_number, balance, currency, status) " +
                   "VALUES (:customerId, :accountNumber, :balance, :currency, :status)", nativeQuery = true)
    void saveAccount(@Param("customerId") Long customerId,
                     @Param("accountNumber") String accountNumber,
                     @Param("balance") BigDecimal balance,
                     @Param("currency") String currency,
                     @Param("status") String status);

    @Query(value = "SELECT * FROM accounts WHERE customer_id = :customerId AND status = 'ACTIVE'", nativeQuery = true)
    List<Account> findActiveByCustomerId(@Param("customerId") Long customerId);
}
