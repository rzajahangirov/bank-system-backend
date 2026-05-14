package com.bank_system.bank_system.repositories;

import com.bank_system.bank_system.models.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    @Query(value = "SELECT * FROM loans WHERE customer_id = :customerId", nativeQuery = true)
    List<Loan> findByCustomerId(@Param("customerId") Long customerId);

    @Query(value = "SELECT * FROM loans WHERE id = :id", nativeQuery = true)
    Optional<Loan> findByIdNative(@Param("id") Long id);

    @Modifying
    @Query(value = "INSERT INTO loans (customer_id, loan_amount, interest_rate, loan_term_months, start_date, end_date, status) " +
                   "VALUES (:customerId, :loanAmount, :interestRate, :termMonths, :startDate, :endDate, :status)", nativeQuery = true)
    void saveLoan(@Param("customerId") Long customerId,
                  @Param("loanAmount") BigDecimal loanAmount,
                  @Param("interestRate") BigDecimal interestRate,
                  @Param("termMonths") Integer termMonths,
                  @Param("startDate") LocalDate startDate,
                  @Param("endDate") LocalDate endDate,
                  @Param("status") String status);

    @Query(value = "SELECT * FROM loans WHERE customer_id = :customerId ORDER BY start_date DESC LIMIT 1", nativeQuery = true)
    Optional<Loan> findLatestByCustomerId(@Param("customerId") Long customerId);

    @Modifying
    @Query(value = "UPDATE loans SET status = :status WHERE id = :id", nativeQuery = true)
    void updateLoanStatus(@Param("id") Long id, @Param("status") String status);

    @Query(value = "SELECT * FROM loans WHERE customer_id = :customerId AND status = 'APPROVED'", nativeQuery = true)
    List<Loan> findActiveByCustomerId(@Param("customerId") Long customerId);
}
